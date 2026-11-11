from __future__ import annotations

import asyncio
import json
import os
import re
import shutil
import subprocess
import uuid
from pathlib import Path
from typing import Any

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

BASE_DIR = Path(__file__).resolve().parent.parent
TEMP_DIR = Path(os.getenv("TEMP_DIR", BASE_DIR / "temp"))
RESULT_DIR = Path(os.getenv("RESULT_DIR", BASE_DIR / "results"))
TEMP_DIR.mkdir(parents=True, exist_ok=True)
RESULT_DIR.mkdir(parents=True, exist_ok=True)

MAX_FILE_BYTES = int(os.getenv("MAX_FILE_BYTES", 50 * 1024 * 1024))
MAX_REFERENCE_FILES = int(os.getenv("MAX_REFERENCE_FILES", 20))
MAX_REQUEST_BYTES = int(os.getenv("MAX_REQUEST_BYTES", 250 * 1024 * 1024))
ENGINE_TIMEOUT_SECONDS = int(os.getenv("ENGINE_TIMEOUT_SECONDS", 1800))
ALLOWED_SUFFIXES = {".mp3", ".wav", ".m4a", ".flac", ".ogg"}

app = FastAPI(title="Konvèti Voice API", version="0.4.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin for origin in os.getenv("CORS_ORIGINS", "").split(",") if origin],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Authorization"],
)


def _is_audio_header(header: bytes) -> bool:
    return (
        header.startswith(b"ID3")
        or (len(header) > 1 and header[0] == 0xFF and (header[1] & 0xE0) == 0xE0)
        or header.startswith((b"RIFF", b"fLaC", b"OggS"))
        or (len(header) >= 8 and header[4:8] == b"ftyp")
    )


async def _save_upload(upload: UploadFile, destination: Path) -> int:
    suffix = Path(upload.filename or "audio.mp3").suffix.lower()
    if suffix not in ALLOWED_SUFFIXES:
        raise HTTPException(status_code=415, detail="Formats acceptés : MP3, WAV, M4A, FLAC et OGG.")

    size = 0
    header = b""
    try:
        with destination.open("wb") as stream:
            while chunk := await upload.read(1024 * 1024):
                size += len(chunk)
                if size > MAX_FILE_BYTES:
                    raise HTTPException(status_code=413, detail="Un fichier dépasse la limite configurée.")
                if not header:
                    header = chunk[:16]
                stream.write(chunk)
    except Exception:
        destination.unlink(missing_ok=True)
        raise
    finally:
        await upload.close()

    if not size or not _is_audio_header(header):
        destination.unlink(missing_ok=True)
        raise HTTPException(status_code=415, detail="Le fichier ne semble pas être un audio pris en charge.")
    return size


def _run_engine(source: Path, refs_manifest: Path, output: Path, fidelity: float, pitch: float) -> None:
    """Exécute un adaptateur local configuré par l’opérateur, sans shell.

    VOICE_CONVERSION_COMMAND est un tableau JSON d’arguments. Variables disponibles :
    {source}, {references_manifest}, {output}, {fidelity}, {pitch}.
    """
    raw_command = os.getenv("VOICE_CONVERSION_COMMAND")
    if not raw_command:
        raise HTTPException(status_code=503, detail="Le moteur vocal n’est pas configuré ; aucune conversion factice ne sera créée.")
    try:
        template: Any = json.loads(raw_command)
    except json.JSONDecodeError as exc:
        raise HTTPException(status_code=500, detail="VOICE_CONVERSION_COMMAND doit être un tableau JSON.") from exc
    if not isinstance(template, list) or not template or not all(isinstance(arg, str) for arg in template):
        raise HTTPException(status_code=500, detail="La commande du moteur doit être un tableau JSON de chaînes.")
    values = {
        "source": str(source.resolve()),
        "references_manifest": str(refs_manifest.resolve()),
        "output": str(output.resolve()),
        "fidelity": str(fidelity),
        "pitch": str(pitch),
    }
    try:
        argv = [arg.format(**values) for arg in template]
    except (KeyError, ValueError) as exc:
        raise HTTPException(status_code=500, detail="Variable inconnue dans la commande du moteur.") from exc
    try:
        process = subprocess.run(argv, capture_output=True, text=True, timeout=ENGINE_TIMEOUT_SECONDS, check=False)
    except subprocess.TimeoutExpired as exc:
        raise HTTPException(status_code=504, detail="Le traitement vocal a dépassé le délai maximal.") from exc
    except OSError as exc:
        raise HTTPException(status_code=500, detail="Impossible de lancer le moteur configuré.") from exc
    if process.returncode != 0:
        # Avoid returning engine logs, which may contain local paths or sensitive data.
        raise HTTPException(status_code=502, detail="Le moteur vocal a échoué ; consultez les journaux côté serveur.")
    if not output.is_file() or output.stat().st_size == 0:
        raise HTTPException(status_code=502, detail="Le moteur n’a pas produit de fichier audio valide.")


@app.get("/health")
def health() -> dict[str, str | bool]:
    return {"status": "ok", "service": "konveti-voice-api", "engine_configured": bool(os.getenv("VOICE_CONVERSION_COMMAND"))}


@app.post("/convert")
async def convert_voice(
    source_audio: UploadFile = File(...),
    reference_audio: list[UploadFile] = File(...),
    language: str = Form("ht"),
    voice_name: str = Form("voice"),
    fidelity: float = Form(0.75),
    pitch: float = Form(0.0),
    consent: bool = Form(False),
):
    if not consent:
        raise HTTPException(status_code=400, detail="Confirmez votre autorisation d’utiliser la voix de référence.")
    if not reference_audio or len(reference_audio) > MAX_REFERENCE_FILES:
        raise HTTPException(status_code=422, detail=f"Ajoutez de 1 à {MAX_REFERENCE_FILES} fichiers de référence.")
    if not 0 <= fidelity <= 1 or not -12 <= pitch <= 12:
        raise HTTPException(status_code=422, detail="Les paramètres de fidélité ou de hauteur sont hors limites.")
    if language not in {"ht", "fr"}:
        raise HTTPException(status_code=422, detail="Langues acceptées : ht (créole haïtien) ou fr.")

    job_id = uuid.uuid4().hex
    work_dir = TEMP_DIR / job_id
    work_dir.mkdir(mode=0o700)
    try:
        source_suffix = Path(source_audio.filename or "source.mp3").suffix.lower()
        source_path = work_dir / f"source{source_suffix}"
        source_size = await _save_upload(source_audio, source_path)
        refs_dir = work_dir / "references"
        refs_dir.mkdir()
        ref_paths: list[Path] = []
        aggregate_size = source_size
        for index, upload in enumerate(reference_audio):
            suffix = Path(upload.filename or "reference.mp3").suffix.lower()
            ref_path = refs_dir / f"ref_{index:02d}{suffix}"
            aggregate_size += await _save_upload(upload, ref_path)
            if aggregate_size > MAX_REQUEST_BYTES:
                raise HTTPException(status_code=413, detail="La taille totale de la requête dépasse la limite configurée.")
            ref_paths.append(ref_path)

        refs_manifest = work_dir / "references.json"
        refs_manifest.write_text(json.dumps([str(path.resolve()) for path in ref_paths]), encoding="utf-8")
        output_path = work_dir / "result.wav"
        await asyncio.to_thread(_run_engine, source_path, refs_manifest, output_path, fidelity, pitch)

        safe_name = re.sub(r"[^\w -]", "", voice_name, flags=re.UNICODE).strip()[:40] or "voice"
        result_path = RESULT_DIR / f"{job_id}_{safe_name}.wav"
        shutil.copyfile(output_path, result_path)
        return {
            "request_id": job_id,
            "status": "completed",
            "reference_count": len(ref_paths),
            "result_url": f"/result/{job_id}",
            "message": "Conversion terminée.",
        }
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)


@app.get("/result/{request_id}")
def get_result(request_id: str):
    if not re.fullmatch(r"[a-f0-9]{32}", request_id):
        raise HTTPException(status_code=400, detail="Identifiant invalide.")
    matches = list(RESULT_DIR.glob(f"{request_id}_*.wav"))
    if not matches:
        raise HTTPException(status_code=404, detail="Résultat introuvable ou supprimé.")
    return FileResponse(matches[0], media_type="audio/wav", filename=matches[0].name)
