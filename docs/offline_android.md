# Mode hors ligne Android

## Exigence produit

L’application doit convertir les fichiers sans réseau après la configuration initiale. Cela impose que le modèle, le runtime d’inférence, le prétraitement audio et l’export fonctionnent sur l’appareil. Le backend FastAPI ne peut pas être utilisé pour une conversion hors ligne.

## État actuel

Le code Android présent est une interface de sélection et de consentement. Il ne contient pas de modèle de conversion, n’effectue pas l’inférence locale et n’envoie pas encore les audios à un service. Les fichiers Gradle de compilation et un APK construit ne sont pas établis par les éléments inspectés. Par conséquent, le dépôt ne fournit pas encore un APK fonctionnel hors ligne.

## Parcours recommandé

1. Choisir et valider un moteur qui accepte les références vocales prévues et le créole haïtien.
2. Mesurer poids du modèle, mémoire et vitesse sur les téléphones cibles. Un modèle RVC habituel peut nécessiter un modèle de voix cible entraîné ; des fichiers de référence seuls ne garantissent pas la conversion à la volée.
3. Choisir un runtime mobile compatible, convertir/optimiser le modèle si nécessaire et vérifier que les licences autorisent la redistribution.
4. Installer le modèle dans le stockage privé de l’application, soit inclus dans l’APK, soit téléchargé une fois avec validation cryptographique, puis rendre l’inférence indépendante du réseau.
5. Construire le flux local : choix source + références, validation, conversion, progression, annulation et export.
6. Tester après téléchargement/configuration en mode avion ; vérifier l’absence de transfert réseau et mesurer les erreurs, la consommation batterie et l’échauffement.
7. Publier l’APK signé avec une politique de mise à jour et de suppression des modèles et données vocales.

## Critère d’acceptation

Après l’installation du modèle et la configuration initiale, activer le mode avion et redémarrer l’application. La sélection des audios, la conversion, l’écoute et l’export doivent réussir sans requête réseau. Si un modèle doit être téléchargé, l’application doit clairement indiquer que la première configuration exige Internet.
