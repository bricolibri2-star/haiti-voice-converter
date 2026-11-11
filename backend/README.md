# backend/README.md

# Backend Haiti Voice Converter

## Démarrage

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Endpoint santé

```bash
curl http://localhost:8000/health
```

## Endpoint de conversion

```bash
curl -X POST http://localhost:8000/convert \
  -F "source_audio=@/path/to/source.mp3" \
  -F "reference_audio=@/path/to/reference.mp3" \
  -F "language=fr" \
  -F "voice_name=demo" \
  -F "fidelity=0.75" \
  -F "pitch=0" \
  -F "consent=true"
```

## Réponse attendue

Le backend renvoie un `request_id` et un `result_file` finalisé ou en cours de traitement.

## Important

Ce backend fournit les points d’entrée. Le vrai moteur de conversion vocale doit être branché ici pour faire la voix finale.
