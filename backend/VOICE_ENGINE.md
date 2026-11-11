# Références vocales multiples

L’application accepte plusieurs fichiers de référence pour une voix cible. Une collection plus large, propre, variée et réellement issue du même locuteur peut aider certains modèles à représenter le timbre de manière plus robuste. Cela ne garantit pas une fidélité parfaite et n’améliore pas tous les moteurs de la même façon.

## Limites appliquées

- Jusqu’à 20 fichiers de référence par requête.
- 50 Mo maximum par fichier ; la limite globale du serveur doit aussi être configurée au niveau du proxy et du stockage temporaire.
- Formats : MP3, WAV, M4A, FLAC, OGG.
- Le backend crée un manifeste JSON des références et le transmet à l’adaptateur via `{references_manifest}`.

Exemple de commande, à adapter à un moteur déjà installé et validé :

```json
["/opt/voice-adapter/run", "--source", "{source}", "--references-manifest", "{references_manifest}", "--output", "{output}", "--fidelity", "{fidelity}", "--pitch", "{pitch}"]
```

La conversion n’est pas activée tant que `VOICE_CONVERSION_COMMAND` n’est pas configuré. Un moteur RVC traditionnel nécessite généralement un modèle cible entraîné ; il ne crée pas automatiquement le timbre à partir d’une liste de références à chaque conversion. Toutes les références doivent être autorisées par les locuteurs concernés.
