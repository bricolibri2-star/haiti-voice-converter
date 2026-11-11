from __future__ import annotations

import json
import os
import shutil
import subprocess
import uuid
from pathlib import Path
from typing import Any

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

BASE_DIR = Path(__file__).resolve().parent.parent
TEMP_DIR = BASE_DIR / "temp"
RESULT_DIR = BASE_DIR / "results"
TEMP_DIR.mkdir(parents=True, exist_ok=True)
RESULT_DIR.mkdir(parents=True, exist_ok=True)
MAX_FILE_BYTES = 50 * 1024 * 1024
MAX_REFERENCE_FILES = 20
ALLOWED_SUFFIXES = {".mp3", ".wav", ".m4a", ".flac", ".ogg"}

app = FastAPI(title="Haiti Voice Converter API", version="0.3.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ORIGINS", "http://localhost").split(","),
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Authorization"],
)


def _looks_like_audio(data: bytes) -> bool:
    return (data.startswith(b"ID3") or (len(data) > 1 and data[0] == 0xFF and data[1] & 0xE0 == 0xE0)
            or data.startswith((b"RIFF", b"fLaC", b"OggS"))
            or (len(data) >= 8 and data[4:8] == b"ftyp"))


async def _save_upload(upload: UploadFile, destination: Path) -> None:
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
                    raise HTTPException(status_code=413, detail="Chaque fichier est limité à 50 Mo.")
                if not header:
                    header = chunk[:16]
                stream.write(chunk)
    except Exception:
        destination.unlink(missing_ok=True)
        raise
    finally:
        await upload.close()
    if size == 0 or not _looks_like_audio(header):
        destination.unlink(missing_ok=True)
        raise HTTPException(status_code=415, detail="Le fichier ne semble pas être un audio pris en charge.")


def _run_conversion(source: Path, references_manifest: Path, output: Path, fidelity: float, pitch: float) -> None:
    """Call an installed reference-conditioned conversion adapter, never a shell command."""
    raw = os.getenv("VOICE_CONVERSION_COMMAND")
    if not raw:
        raise HTTPException(status_code=503, detail="Le moteur vocal n’est pas configuré ; aucune conversion factice n’est produite.")
    try:
        template: Any = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise HTTPException(status_code=500, detail="VOICE_CONVERSION_COMMAND doit être un tableau JSON.") from exc
    if not isinstance(template, list) or not template or not all(isinstance(arg, str) for arg in template):
        raise HTTPException(status_code=500, detail="La commande du moteur doit être un tableau JSON de chaînes.")
    values = {
        "source": str(source.resolve()),
        "references_manifest": str(references_manifest.resolve()),
        "output": str(output.resolve()),
        "fidelity": str(fidelity),
        "pitch": str(pitch),
    }
    argv = [arg.format(**values) for arg in template]
    try:
        process = subprocess.run(argv, capture_output=True, text=True, timeout=1800, check=False)
    except subprocess.TimeoutExpired as exc:
        raise HTTPException(status_code=504, detail="Le traitement vocal a dépassé le délai maximal.") from exc
    except OSError as exc:
        raise HTTPException(status_code=500, detail="Impossible de lancer le moteur configuré.") from exc
    if process.returncode != 0:
        raise HTTPException(status_code=502, detail="Le moteur vocal a échoué ; consultez les journaux côté serveur.")
    if not output.is_file() or output.stat().st_size == 0:
        raise HTTPException(status_code=502, detail="Le moteur n’a pas produit de fichier audio valide.")


@app.get("/health")
def health():
    return {"status": "ok", "engine_configured": bool(os.getenv("VOICE_CONVERSION_COMMAND"))}


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
        raise HTTPException(status_code=400, detail="L’autorisation d’utiliser la voix de référence est requise.")
    if not reference_audio or len(reference_audio) > MAX_REFERENCE_FILES:
        raise HTTPException(status_code=422, detail=f"Ajoutez de 1 à {MAX_REFERENCE_FILES} fichiers de référence.")
    if not 0 <= fidelity <= 1 or not -12 <= pitch <= 12:
        raise HTTPException(status_code=422, detail="Paramètres fidélité/hauteur hors limites.")
    if language not in {"ht", "fr"}:
        raise HTTPException(status_code=422, detail="Langues acceptées : ht ou fr.")

    job_id = uuid.uuid4().hex
    work_dir = TEMP_DIR / job_id
    work_dir.mkdir(mode=0o700)
    output = work_dir / "result.wav"
    result_path: Path | None = None
    try:
        source_path = work_dir / "source" / (Path(source_audio.filename or "source.mp3").suffix.lower() or ".mp3")
        source_path.parent.mkdir()
        await _save_upload(source_audio, source_path)
        refs_dir = work_dir / "references"
        refs_dir.mkdir()
        ref_paths: list[Path] = []
        total_refs = 0
        for index, upload in enumerate(reference_audio):
            suffix = Path(upload.filename or "reference.mp3").suffix.lower()
            ref_path = refs_dir / f"ref_{index:02d}{suffix}"
            await _save_upload(upload, ref_path)
            total_refs += ref_path.stat().st_size
            if total_refs > MAX_FILE_BYTES * MAX_REFERENCE_FILES:
                raise HTTPException(status_code=413, detail="La taille totale des références dépasse la limite serveur.")
            ref_paths.append(ref_path)
        manifest = work_dir / "references.json"
        manifest.write_text(json.dumps([str(path.resolve()) for path in ref_paths]), encoding="utf-8")
        _run_conversion(source_path, manifest, output, fidelity, pitch)
        safe_name = "".join(c for c in voice_name if c.isalnum() or c in "-_ ").strip()[:40] or "voice"
        result_path = RESULT_DIR / f"{job_id}_{safe_name}.wav"
        shutil.copyfile(output, result_path)
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
    if not request_id.isalnum():
        raise HTTPException(status_code=400, detail="Identifiant invalide.")
    matches = list(RESULT_DIR.glob(f"{request_id}_*.wav"))
    if not matches:
        raise HTTPException(status_code=404, detail="Résultat introuvable.")
    return FileResponse(matches[0], media_type="audio/wav", filename=matches[0].name)
