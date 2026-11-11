# Architecture du projet

## 1. Couches du système

### Application Android

Responsable de :
- choisir le fichier source,
- choisir les fichiers de référence,
- demander le consentement,
- envoyer la demande de conversion,
- afficher l’état de traitement,
- télécharger et lire le résultat.

### Backend API

Responsable de :
- validation des fichiers audio,
- contrôle de taille, format et consentement,
- stockage temporaire,
- orchestration de la conversion,
- renvoi du fichier final.

### Moteur vocal

Responsable de :
- appliquer le timbre d’une voix cible,
- préserver le rythme et la prosodie de la source,
- produire un fichier audio final exploitable.

## 2. Schéma logique

```
Android App
    ├─ source audio
    ├─ 1..N référence vocale(s)
    ├─ consentement
    └─ paramètres
        ↓
FastAPI backend
    ├─ validation
    ├─ stockage temporaire
    ├─ appel moteur vocal
    └─ résultat WAV/MP3
        ↓
Utilisateur / téléchargement
```

## 3. Points de vigilance

- La qualité du produit dépend de la qualité des références.
- Un seul fichier de référence n’est souvent pas suffisant pour capturer un timbre complexe.
- Le moteur vocal doit être installé et testé côté serveur.
- Une vraie conversion ne doit pas être simulée.

## 4. Règles métier

- Une source audio apporte le contenu et le débit vocal.
- Une référence apporte le timbre.
- Plus il y a de bonnes références, plus le timbre peut être capté correctement.
- Le consentement est obligatoire pour toute voix de référence.
