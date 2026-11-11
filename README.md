# Konvèti Voice Studio

Application Android + backend pour conversion vocale voix-à-voix avec un moteur configurable.

## Objectif

L’application doit permettre à un utilisateur haïtien de téléverser :
- un fichier audio source : paroles, rythme, intonation à conserver,
- un ou plusieurs fichiers de référence : timbre vocal à reproduire.

Le backend valide les fichiers, exige le consentement du locuteur de la voix de référence, puis transmet les fichiers à un moteur vocal configuré. La conversion doit conserver la performance de la source tout en appliquant les caractéristiques vocales de la référence.

## Architecture

- `android/` : application Android Kotlin / Jetpack Compose
- `backend/` : API FastAPI
- `docs/` : documentation de mise en production et de contrôle d’usage

## Ce que le projet contient déjà

- sélection d’un audio source,
- sélection de plusieurs références vocales,
- confirmation de consentement,
- réglage visuel pour la fidélité et la hauteur,
- backend API de validation d’upload,
- point d’entrée principal `/convert` prêt à brancher un moteur réel,
- politique de stockage temporaire et de résultat.

## Ce qui est encore nécessaire pour un vrai produit

- un moteur vocal réel installé et configuré,
- des références de bonne qualité issues d’un locuteur autorisé,
- un test de qualité sur des voix haïtiennes réelles,
- intégration Android du flux HTTP complet,
- génération de l’APK final.

## Sécurité et éthique

- le consentement explicite est requis pour utiliser une voix de référence,
- la conversion ne doit pas être utilisée pour manipuler ou tromper,
- les fichiers audio doivent être supprimés selon une politique de rétention,
- l’utilisation des données doit être documentée et limitée.

## Licence

MIT
