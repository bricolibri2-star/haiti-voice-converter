# Données vocales haïtiennes : collecte et évaluation

## Objectif

Construire et tester une conversion voix-à-voix adaptée à des locuteurs haïtiens natifs, en créole haïtien (Kreyòl) et, si le produit le prévoit, en français parlé par des Haïtiens. La voix source doit apporter le contenu et la performance vocale ; les références autorisées servent à caractériser la voix cible. La qualité dépend du moteur choisi, des données et de leur qualité : davantage de fichiers n’améliore pas automatiquement le résultat.

## 1. Consentement et gouvernance

Avant tout enregistrement ou traitement :

- obtenir un accord explicite, compréhensible et documenté du locuteur ; préciser l’usage de conversion vocale, la durée de conservation, les personnes ayant accès aux données et les conditions de suppression ;
- indiquer séparément si les enregistrements peuvent servir à l’entraînement, à l’évaluation ou uniquement à une conversion ponctuelle ;
- ne pas réutiliser les données pour un autre locuteur ou projet sans nouvel accord ;
- fournir un moyen de retirer l’autorisation et supprimer les enregistrements ainsi que les dérivés concernés lorsque cela est possible ;
- éviter la collecte de données sensibles ou non nécessaires, limiter les accès et chiffrer stockage et transferts en production ;
- ne pas créer de voix de personnes sans leur accord ni présenter une voix générée comme un enregistrement authentique.

Conserver le texte du consentement, la date, le périmètre accordé et l’identifiant du profil de voix dans un registre séparé des fichiers audio. Ne pas enregistrer d’informations personnelles dans les noms de fichiers.

## 2. Protocole d’enregistrement

Demander au locuteur de parler naturellement, dans la langue et la variété qu’il souhaite représenter. Pour chaque profil cible :

- enregistrer plusieurs extraits séparés, avec des phrases et sons variés ; éviter de ne collecter que la même phrase répétée ;
- garder une voix et un environnement cohérents par profil ; ne jamais mélanger les extraits de plusieurs personnes sous une identité de voix unique ;
- privilégier un endroit calme, un microphone stable, une distance constante et une absence de musique ou d’effets ;
- inclure, avec l’accord du locuteur, des styles de parole variés sans lui demander d’imiter un accent stéréotypé ;
- noter la langue/variété déclarée, les conditions d’enregistrement et les problèmes observés, sans inférer l’identité à partir de la voix.

Une meilleure diversité peut aider à représenter les variations naturelles d’un locuteur, mais la quantité seule ne garantit ni la ressemblance ni la qualité. Prévoir un contrôle d’écoute et retirer les extraits bruités, coupés ou contenant plusieurs locuteurs.

## 3. Préparation et séparation des données

1. Vérifier format, durée, intégrité, écrêtage, silence excessif et présence de parole.
2. Écouter des échantillons ; ne pas se fier uniquement aux métadonnées du fichier.
3. Normaliser le format avec une chaîne reproductible documentée par le moteur choisi ; conserver les originaux séparément jusqu’à la fin de la période de conservation autorisée.
4. Constituer des ensembles distincts d’entraînement, de validation et de test. Éviter qu’un même extrait ou une version recoupée apparaisse dans plusieurs ensembles.
5. Associer chaque fichier à un identifiant pseudonyme de locuteur et à l’autorisation correspondante.
6. Documenter les transformations, versions de modèle et empreintes des fichiers nécessaires à la reproductibilité, sans publier les voix privées.

La méthode exacte (modèle de conversion à référence, modèle cible entraîné, formats audio requis et quantité de données) doit être déterminée après sélection et test d’un moteur compatible. Les modèles RVC classiques peuvent nécessiter un modèle cible pré-entraîné et ne sont pas automatiquement des systèmes zéro-shot à partir d’une liste de références.

## 4. Évaluation avant lancement

Évaluer séparément :

- **ressemblance de timbre** : appréciation par le locuteur cible et des évaluateurs autorisés ;
- **intelligibilité et prononciation** : évaluateurs parlant la variété concernée, notamment le créole haïtien ;
- **préservation de la source** : rythme, pauses, intonation et contenu ;
- **qualité audio** : artefacts, coupures, bruit, stabilité du volume ;
- **robustesse** : différents locuteurs consentants, durées et conditions d’enregistrement.

Utiliser des évaluations d’écoute contrôlées et documenter les limites. Ne pas prétendre qu’un score automatique ou l’augmentation du nombre de références prouve une fidélité parfaite. Faire valider les résultats par les personnes concernées avant de les utiliser publiquement.

## 5. Mise en production

- Déployer le moteur isolé du serveur API avec authentification, quotas, TLS et limites d’upload configurées également au niveau du proxy.
- Rendre les fichiers privés ; définir une durée de rétention courte et une suppression automatique des sources, références, résultats et fichiers temporaires.
- Journaliser les identifiants de tâche et erreurs techniques, pas les contenus audio ni les liens publics permanents.
- Prévoir une procédure de retrait d’un profil vocal et de suppression des modèles dérivés, dans les limites techniques et contractuelles applicables.
- Commencer par un pilote fermé avec des locuteurs natifs volontaires ; ne pas annoncer un produit prêt avant les tests de qualité, consentement et sécurité.

## Checklist de démarrage

- [ ] Moteur choisi et licence vérifiée.
- [ ] Consentement distinct pour conversion, entraînement et évaluation.
- [ ] Locuteurs natifs volontaires recrutés sans pression et variété linguistique documentée.
- [ ] Enregistrements contrôlés et attribués à un seul profil.
- [ ] Ensembles de test séparés et évaluation d’écoute prévue.
- [ ] Rétention, retrait, sécurité et purge configurés.
- [ ] Résultats du pilote validés avec les locuteurs concernés.
