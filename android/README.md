# Application Android — état hors ligne

L’interface actuelle est un prototype : elle permet la sélection des fichiers et l’affichage du consentement, mais n’intègre pas encore le moteur vocal ni la conversion Android vers le backend. Les fichiers Gradle nécessaires à la compilation d’un APK doivent également être présents dans un projet Android complet.

## Objectif hors ligne

Une fois le modèle et les ressources installés sur l’appareil, la conversion doit fonctionner en mode avion : audio source, références vocales, prétraitement, inférence et export sont traités localement. Un backend distant ne peut pas fournir une conversion sans connexion.

Deux options de distribution sont possibles :

1. **Modèle inclus dans l’installation** : prêt hors ligne dès l’installation, mais APK/AAB potentiellement volumineux et soumis aux licences de poids du modèle.
2. **Téléchargement initial du modèle** : Internet nécessaire une seule fois pour l’obtenir et vérifier son empreinte ; après installation locale, la conversion fonctionne hors ligne. Prévoir téléchargement repris, espace libre, stockage privé et suppression/remplacement du modèle.

## Travail technique restant

- choisir un modèle de conversion réellement conditionné par les références ou un modèle cible pré-entraîné ;
- confirmer licence, poids, formats, qualité créole haïtien, performance et mémoire sur les appareils visés ;
- porter l’inférence vers un runtime Android compatible (par exemple ONNX Runtime Mobile ou TensorFlow Lite si le modèle s’y prête) ;
- concevoir un pipeline audio local et une tâche de fond Android avec progression, annulation et contrôle thermique/mémoire ;
- intégrer lecture/export du résultat et vérifier qu’aucune requête audio n’est envoyée au réseau en mode local ;
- fournir les fichiers Gradle, construire l’APK, puis tester en mode avion sur plusieurs téléphones.

**Aucun APK de conversion hors ligne n’est prêt à ce stade.** La présence d’une interface Android ne signifie pas que le modèle ou le moteur est embarqué. Ne pas intégrer une clé API serveur dans l’APK : les secrets extraits d’une application cliente ne peuvent pas être protégés de manière fiable.
