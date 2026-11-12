from __future__ import annotations

import math
import os
import shutil
import subprocess
from pathlib import Path

from fastapi import HTTPException


def apply_pitch_effect(source: Path, output: Path, semitones: float) -> None:
    """Create a real pitch-shifted audio file with FFmpeg, preserving its duration.

    This is a basic audio effect, NOT voice conversion: reference recordings are
    deliberately not used to imitate or reproduce a speaker's identity.
    """
    binary = os.getenv("FFMPEG_BINARY", "ffmpeg")
    if not shutil.which(binary):
        raise HTTPException(
            status_code=503,
            detail="Le moteur d’effet audio FFmpeg est absent. Installez FFmpeg ou configurez VOICE_CONVERSION_COMMAND.",
        )
    if not -12.0 <= semitones <= 12.0:
        raise HTTPException(status_code=422, detail="La hauteur doit être comprise entre -12 et +12 demi-tons.")

    factor = 2 ** (semitones / 12.0)
    # FFmpeg atempo accepts factors in [0.5, 2.0]; our bounded pitch range
    # keeps the inverse factor inside those limits.
    filters = f"asetrate=44100*{factor:.8f},aresample=44100,atempo={1.0 / factor:.8f}"
    try:
        process = subprocess.run(
            [binary, "-nostdin", "-v", "error", "-y", "-i", str(source), "-vn", "-af", filters,
             "-acodec", "pcm_s16le", "-ar", "44100", str(output)],
            capture_output=True,
            text=True,
            timeout=int(os.getenv("ENGINE_TIMEOUT_SECONDS", "1800")),
            check=False,
        )
    except subprocess.TimeoutExpired as exc:
        output.unlink(missing_ok=True)
        raise HTTPException(status_code=504, detail="L’effet audio a dépassé le délai maximal.") from exc
    except OSError as exc:
        output.unlink(missing_ok=True)
        raise HTTPException(status_code=503, detail="Impossible de démarrer FFmpeg.") from exc

    if process.returncode != 0 or not output.is_file() or output.stat().st_size == 0:
        output.unlink(missing_ok=True)
        raise HTTPException(status_code=422, detail="FFmpeg n’a pas pu lire ou transformer cet audio.")
