# Backend Konvèti Voice

## État

L’API FastAPI reçoit un audio source et plusieurs références vocales, vérifie les fichiers et le consentement, puis appelle l’adaptateur configuré par `VOICE_CONVERSION_COMMAND`. Sans moteur configuré, `/convert` renvoie une erreur 503 et ne fabrique pas de résultat.

Cette API est une option **en ligne**. Elle n’est pas nécessaire au mode hors ligne Android, qui exigera une intégration de modèle et d’inférence exécutée localement sur le téléphone.

## Installation locale

```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Windows : .venv\\Scripts\\activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

## Configuration moteur

Définir `VOICE_CONVERSION_COMMAND` comme un tableau JSON d’arguments sans shell. L’adaptateur doit accepter `{source}`, `{references_manifest}`, `{output}`, `{fidelity}` et `{pitch}`. Consulter `VOICE_ENGINE.md` pour le contrat et les limites des modèles RVC classiques.

## Sécurité de déploiement

La clé `API_TOKEN` est actuellement facultative dans le code : définissez-la pour tout usage privé et ne la mettez jamais dans l’APK. Pour une exposition publique, il faut une authentification par utilisateur, TLS, quotas distribués, limites au proxy, isolation du moteur et politique de rétention testée. La configuration de sécurité Android n’empêche pas les appels directs au backend.
