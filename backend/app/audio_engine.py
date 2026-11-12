"""Small FFmpeg-backed audio-effects engine for the API's basic mode.

This performs real, audible DSP (pitch/speed shift plus compression); it is NOT
speaker identity conversion and does not use reference recordings to clone a voice.
A trained/authorized voice-conversion adapter can still be selected with
VOICE_CONVERSION_COMMAND.
"""
from __future__ import annotations

import math
import shutil
import subprocess
import wave
from pathlib import Path


class AudioEngineError(RuntimeError):
    """The local audio processor is unavailable or failed safely."""


def is_available() -> bool:
    return shutil.which("ffmpeg") is not None


def _filter(fidelity: float, pitch: float) -> str:
    if not 0.0 <= fidelity <= 1.0 or not -12.0 <= pitch <= 12.0:
        raise ValueError("Audio parameters are outside supported limits")
    # Changing the playback sample rate shifts both pitch and speed. It is an
    # intentionally simple, transparent effect—not pitch-preserving conversion.
    rate = 2 ** (pitch / 12.0)
    ratio = 1.0 + 5.0 * fidelity
    return (
        f"asetrate=44100*{rate:.8f},aresample=44100:resampler=soxr:precision=20,"
        f"acompressor=threshold=-18dB:ratio={ratio:.3f}:attack=10:release=120,"
        "alimiter=limit=0.95"
    )


def transform(source: Path, output: Path, fidelity: float, pitch: float, timeout: int = 1800) -> None:
    """Transform supported audio to a PCM WAV using the system FFmpeg binary."""
    binary = shutil.which("ffmpeg")
    if not binary:
        raise AudioEngineError("FFmpeg is not installed; install ffmpeg to enable basic audio effects.")
    if not source.is_file() or source.stat().st_size == 0:
        raise AudioEngineError("Source audio is missing or empty.")
    output.parent.mkdir(parents=True, exist_ok=True)
    command = [
        binary, "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
        "-i", str(source), "-map", "0:a:0", "-vn", "-af", _filter(fidelity, pitch),
        "-ar", "44100", "-acodec", "pcm_s16le", str(output),
    ]
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout, check=False)
    except subprocess.TimeoutExpired as exc:
        output.unlink(missing_ok=True)
        raise AudioEngineError("Audio processing timed out.") from exc
    except OSError as exc:
        output.unlink(missing_ok=True)
        raise AudioEngineError("Unable to start FFmpeg.") from exc
    if result.returncode != 0:
        output.unlink(missing_ok=True)
        raise AudioEngineError("FFmpeg could not decode or transform the uploaded audio.")
    try:
        with wave.open(str(output), "rb") as wav:
            if wav.getnframes() <= 0 or wav.getnchannels() <= 0 or wav.getframerate() <= 0:
                raise AudioEngineError("FFmpeg produced an invalid or empty WAV file.")
    except (wave.Error, OSError) as exc:
        output.unlink(missing_ok=True)
        raise AudioEngineError("FFmpeg did not produce a valid PCM WAV file.") from exc


def _validate_parameters(fidelity: float, pitch: float) -> None:
    """Exposed for unit testing parameter validation without invoking FFmpeg."""
    if not math.isfinite(fidelity) or not math.isfinite(pitch):
        raise ValueError("Audio parameters must be finite")
    _filter(fidelity, pitch)
