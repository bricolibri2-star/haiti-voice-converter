from __future__ import annotations

import json
import os
import shutil
import uuid
from pathlib import Path
from typing import List

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse

BASE_DIR = Path(__file__).resolve().parent.parent
UPLOAD_DIR = BASE_DIR / "uploads"
RESULT_DIR = BASE_DIR / "results"
TEMP_DIR = BASE_DIR / "temp"

UPLOAD_DIR.mkdir(exist_ok=True)
RESULT_DIR.mkdir(exist_ok=True)
TEMP_DIR.mkdir(exist_ok=True)

app = FastAPI(title="Haiti Voice Converter API", version="0.2.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "haiti-voice-converter"}


@app.post("/convert")
async def convert_voice(
    source_audio: UploadFile = File(...),
    reference_audio: UploadFile = File(...),
    language: str = Form("fr"),
    voice_name: str = Form("default"),
    fidelity: float = Form(0.75),
    pitch: float = Form(0.0),
    consent: bool = Form(False),
):
    if not consent:
        raise HTTPException(status_code=400, detail="Le consentement pour la voix de référence est requis.")

    request_id = uuid.uuid4().hex
    request_dir = TEMP_DIR / request_id
    request_dir.mkdir(exist_ok=True)

    source_path = request_dir / "source.mp3"
    reference_path = request_dir / "reference.mp3"
    result_path = request_dir / "output.wav"

    with source_path.open("wb") as source_file:
        shutil.copyfileobj(source_audio.file, source_file)

    with reference_path.open("wb") as ref_file:
        shutil.copyfileobj(reference_audio.file, ref_file)

    metadata = {
        "request_id": request_id,
        "language": language,
        "voice_name": voice_name,
        "fidelity": fidelity,
        "pitch": pitch,
        "source_file": str(source_path),
        "reference_file": str(reference_path),
        "consent": consent,
        "status": "queued",
    }

    metadata_path = request_dir / "request.json"
    metadata_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")

    # Placeholder logic: a real conversion model would be called here.
    # This is the integration point for RVC / So-VITS-SVC / any own conversion pipeline.
    # For now, we keep a stub and write a placeholder output.
    result_path.write_bytes(b"")

    final_output = RESULT_DIR / f"{request_id}_{voice_name}.wav"
    shutil.copy(result_path, final_output)

    return JSONResponse(
        status_code=200,
        content={
            "request_id": request_id,
            "status": "processing",
            "language": language,
            "voice_name": voice_name,
            "fidelity": fidelity,
            "pitch": pitch,
            "source_file": source_path.name,
            "reference_file": reference_path.name,
            "result_file": final_output.name,
            "message": "Conversion acceptée. Connecter un vrai moteur vocal pour générer un résultat final.",
        },
    )


@app.get("/result/{request_id}")
def get_result(request_id: str):
    candidate = list(RESULT_DIR.glob(f"{request_id}_*.wav"))
    if not candidate:
        raise HTTPException(status_code=404, detail="Résultat non trouvé.")
    return FileResponse(candidate[0], media_type="audio/wav", filename=candidate[0].name)


@app.get("/jobs")
def list_jobs():
    jobs = []
    for file in RESULT_DIR.glob("*.wav"):
        jobs.append({"file": file.name, "path": str(file)})
    return {"jobs": jobs}
