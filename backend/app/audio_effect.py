from __future__ import annotations

import math
import shutil
import subprocess
from pathlib import Path


OUTPUT_SAMPLE_RATE = 44_100


def _pitch_filter(semitones: float) -> str:
    """Build a basic pitch-shift filter; this does not change speaker identity."""
    if not math.isfinite(semitones) or not -12.0 <= semitones <= 12.0:
        raise ValueError("pitch must be a finite value between -12 and 12 semitones")
    ratio = 2.0 ** (semitones / 12.0)
    shifted_rate = round(OUTPUT_SAMPLE_RATE * ratio)
    # Resample first to a known rate, change the playback rate for pitch, then
    # resample back and compensate tempo. atempo stays within FFmpeg's 0.5–2 range.
    return (
        f"aresample={OUTPUT_SAMPLE_RATE},"
        f"asetrate={shifted_rate},"
        f"aresample={OUTPUT_SAMPLE_RATE},"
        f"atempo={1.0 / ratio:.10f}"
    )


def transform_audio(source: Path, output: Path, semitones: float) -> None:
    """Decode audio and write a pitch-shifted PCM WAV using system FFmpeg.

    This is an audio effect only, not voice conversion, voice cloning, or
    speaker-identity transfer. FFmpeg must be installed by the server operator.
    """
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise RuntimeError("FFmpeg is not installed on the server.")
    output.parent.mkdir(parents=True, exist_ok=True)
    command = [
        ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
        "-i", str(source), "-vn", "-af", _pitch_filter(semitones),
        "-ar", str(OUTPUT_SAMPLE_RATE), "-c:a", "pcm_s16le", "-f", "wav", str(output),
    ]
    try:
        subprocess.run(command, check=True, capture_output=True, text=True, timeout=1800)
    except subprocess.TimeoutExpired as exc:
        raise RuntimeError("Audio processing timed out.") from exc
    except subprocess.CalledProcessError as exc:
        raise RuntimeError("FFmpeg could not decode or transform the supplied audio.") from exc
    if not output.is_file() or output.stat().st_size <= 44:
        raise RuntimeError("FFmpeg did not create a valid WAV output.")
