# API Konvèti Voice

## Fonctionnement

`POST /convert` reçoit un fichier source sous `source_audio` et un ou plusieurs fichiers de référence sous le même champ répété `reference_audio`. Il exige `consent=true`, accepte les langues `ht` et `fr`, vérifie les extensions, signatures de fichier, limites de taille et paramètres, puis transmet les fichiers à l’adaptateur vocal configuré. Les références temporaires sont supprimées après le traitement ; le résultat est servi par `GET /result/{request_id}`.

Ce serveur ne simule pas de conversion : si aucun moteur n’est configuré, la requête répond `503`.

## Installation locale

```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Windows : .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Contrôle : `GET http://127.0.0.1:8000/health`.

## Connecter l’adaptateur vocal

Installer et tester séparément un moteur compatible puis définir `VOICE_CONVERSION_COMMAND` comme un tableau JSON d’arguments, sans shell. L’adaptateur doit recevoir le chemin source, un manifeste JSON listant les références, le chemin de sortie WAV, la fidélité et le décalage de hauteur. Exemple de forme (le chemin et le programme sont à fournir par l’administrateur) :

```json
["/opt/voice-adapter/run", "--source", "{source}", "--references-manifest", "{references_manifest}", "--output", "{output}", "--fidelity", "{fidelity}", "--pitch", "{pitch}"]
```

Variables de configuration : `MAX_FILE_BYTES` (50 Mio par défaut), `MAX_REFERENCE_FILES` (20), `MAX_REQUEST_BYTES` (250 Mio), `ENGINE_TIMEOUT_SECONDS` (1800), `TEMP_DIR`, `RESULT_DIR`, `CORS_ORIGINS` (origines séparées par virgules).

**Précision modèle :** les modèles RVC classiques utilisent habituellement un modèle cible pré-entraîné. Ils ne créent pas automatiquement une voix cible à partir de plusieurs références lors de chaque requête. Le moteur branché doit explicitement prendre en charge la conversion conditionnée par référence, ou le locuteur doit disposer d’un modèle cible préalablement entraîné avec son autorisation. La quantité seule ne garantit pas la fidélité : qualité, diversité, cohérence du locuteur et modèle comptent aussi.

## Confidentialité et déploiement

- N’exposez pas le serveur directement à Internet sans authentification, chiffrement TLS, limitation de débit et quotas.
- Configurez les limites d’upload également dans le proxy HTTP.
- Les fichiers résultat restent dans `RESULT_DIR` ; configurez une purge automatique/rétention et un stockage privé avant déploiement.
- Collectez le consentement explicite et ne traitez que les voix que l’utilisateur est autorisé à utiliser.
- Les fichiers audio ne sont pas chiffrés par cette version ; utilisez un disque privé et un stockage sécurisé.
