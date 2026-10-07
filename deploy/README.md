# Déploiement Valimbavaka

Comment le code arrive jusqu'au client, et quoi faire quand quelque chose se passe mal.

## Vue d'ensemble

```
PR vers develop ──► CI (tests backend sur Postgres 16, lint + tests + build Angular)
        │
merge sur develop ──► images :develop sur GHCR ──► QA : Njara teste sur SON poste
        │
release vX.Y.Z publiée (Randy, après validation de Njara)
        │
        ├─► CI complète + images :vX.Y.Z ──► STAGING sur l'EliteBook (ton oncle teste)
        │
        └─► 3 jours plus tard, si la release n'est pas bloquée ──► PROD sur l'EliteBook
```

| Environnement | Qui teste | Mis à jour | Où | Adresse |
|---|---|---|---|---|
| QA | Njara | à chaque merge sur `develop` (elle tire les images) | poste de Njara | http://localhost:8090 |
| Staging (UAT) | le client | à chaque release, puis figé | EliteBook | http://localhost:8081 |
| Prod | usage réel | 3 jours après la release | EliteBook | http://localhost |

Règles :

- **L'EliteBook ne compile jamais rien.** Les images sont construites sur les runners GitHub et
  l'EliteBook ne fait que les tirer. Seul le dossier `deploy/` y est récupéré, jamais le code source.
- **Le client ne teste jamais en prod.** La prod contient ses vraies données de facturation.
- **Changesets Liquibase uniquement additifs.** Revenir à une image précédente ne restaure pas la
  base (voir « Retour arrière »).

## Les workflows

| Fichier | Déclencheur | Rôle |
|---|---|---|
| `ci.yml` | chaque PR vers `develop` ou `main` | `mvn verify` (tests + checkstyle) sur Postgres 16, lint + tests + build Angular |
| `images.yml` | merge sur `develop`, release publiée | images `:develop` ; pour une release : CI, images `:vX.Y.Z`, déploiement du staging |
| `deploy-prod-auto.yml` | lun-sam à 9 h, 12 h, 15 h, 18 h (Madagascar) | met en prod la dernière release si elle a 3 jours, tourne en staging et n'est pas bloquée |
| `deploy.yml` | appelé par les deux précédents, ou à la main | déploie une version précise sur `staging` ou `prod` via le runner `workstation` |

### Pourquoi pas les « Environments » GitHub avec un délai d'attente ?

Sur un dépôt **privé** avec le plan gratuit, GitHub ne fournit ni les délais d'attente
(wait timer), ni les validations obligatoires, ni les secrets d'environnement. Le délai de 3 jours
est donc géré par `deploy-prod-auto.yml`, et les mots de passe restent dans les fichiers
`.env.staging` / `.env.prod` de l'EliteBook, jamais sur GitHub.

## Publier une release (Randy)

1. Njara a validé en QA tout ce qui est dans `develop`.
2. Merger `develop` dans `main` (PR de release).
3. GitHub → Releases → *Draft a new release* → tag `vX.Y.Z` sur `main` → **Publish release**.
4. Le staging est mis à jour dans les minutes qui suivent (onglet Actions → « Images Docker »).
5. Prévenir le client : 3 jours pour tester sur http://localhost:8081.

Ne pas cocher « Set as a pre-release » à la publication : une pre-release est considérée comme
bloquée et ne partira jamais en prod.

## Bloquer une mise en prod

Le client a trouvé un problème pendant ses 3 jours de test ? Éditer la release sur GitHub et
cocher **Set as a pre-release**. Elle reste en staging mais ne partira pas en prod.
Pour la débloquer, décocher la case : elle partira au prochain passage (si les 3 jours sont écoulés).

Publier une nouvelle release remet le compteur à zéro : c'est la nouvelle qui devra attendre 3 jours.

## Retour arrière

Actions → **Deployer** → *Run workflow* → environnement + version précédente (ex. `v0.2.0`).

Le script revient aussi **tout seul** à la version précédente si le backend d'une nouvelle
version ne répond pas « UP » sur `/actuator/health` dans les 4 minutes.

Revenir à une image précédente ne touche pas la base : les changesets Liquibase déjà appliqués
restent. Avec des changesets additifs (nouvelles tables ou colonnes), l'ancienne version
fonctionne quand même, car `ddl-auto=validate` accepte les tables et colonnes en plus. Si une
release a supprimé ou renommé quelque chose, il faut aussi restaurer une sauvegarde.

Le même workflow sert à mettre en prod tout de suite un correctif urgent, sans attendre 3 jours.

L'EliteBook garde en local les images de ses 10 dernières versions. Si la version visée y est
encore, rien n'est téléchargé : un retour arrière fonctionne même sans connexion internet.

## Sauvegardes (prod)

- **Automatique** : toutes les 30 minutes, 48 gardées (24 h), fichiers `auto-AAAAMMJJ-HHMM.sql.gz`.
- **Avant chaque mise en prod** : `avant-vX.Y.Z-….sql.gz`, jamais supprimées automatiquement.
  Si cette sauvegarde échoue, le déploiement est annulé avant de toucher quoi que ce soit.

Dossier : la valeur de `BACKUP_DIR` dans `~/valimbavaka/.env.prod`. Copier régulièrement ce
dossier hors du poste (clé USB, Drive) : une sauvegarde sur le même disque ne protège pas d'une
panne du disque.

### Restaurer une sauvegarde

À faire sur l'EliteBook (WSL), en dernier recours et en connaissant la sauvegarde visée :

```bash
cd ~/valimbavaka
P="docker compose -p valimbavaka-prod --env-file .env.prod -f deploy/docker-compose.yml -f deploy/docker-compose.prod.yml"
export TAG=$(cat state/prod.tag)
$P stop core backup
$P exec -T db psql -U valimbavaka -d postgres -c "DROP DATABASE valimbavaka WITH (FORCE);" -c "CREATE DATABASE valimbavaka;"
gunzip -c backups/prod/FICHIER.sql.gz | $P exec -T db psql -U valimbavaka -d valimbavaka
$P start core backup
```

(`deploy/` ici = une copie du dossier `deploy/` du dépôt ; le runner en a une dans son dossier
de travail.)

## Installation de l'EliteBook (une seule fois)

Déjà fait : `.wslconfig` (10 Go), Docker Engine dans WSL2 Ubuntu, `docker login ghcr.io`.

1. **Fichiers d'environnement** dans WSL :
   ```bash
   mkdir -p ~/valimbavaka/backups/prod ~/valimbavaka/state
   # copier deploy/env.example deux fois, puis remplir :
   #   ~/valimbavaka/.env.staging : SPRING_PROFILE=staging, HTTP_PORT=8081
   #   ~/valimbavaka/.env.prod    : SPRING_PROFILE=prod,    HTTP_PORT=80,
   #                                BACKUP_DIR=/home/<utilisateur>/valimbavaka/backups/prod
   # Mots de passe différents pour staging et prod.
   chmod 600 ~/valimbavaka/.env.*
   ```
   Les anciens fichiers compose créés à la main en septembre ne servent plus : ceux du dépôt
   (`deploy/`) les remplacent.
2. **Runner GitHub** : Settings → Actions → Runners → *New self-hosted runner* → Linux x64.
   Suivre les commandes dans WSL, et à l'étape `config.sh` ajouter le label `workstation`.
   Puis l'installer comme service pour qu'il démarre avec WSL :
   ```bash
   sudo ./svc.sh install && sudo ./svc.sh start
   ```
3. **Test à vide** : publier une release `v0.0.1` du socle actuel, vérifier le staging,
   forcer la prod avec *Deployer* (prod, `v0.0.1`), puis tester un retour arrière.

## Environnement QA (Njara)

Prérequis : Docker Desktop, le dépôt cloné, et une connexion à GHCR (une seule fois) avec un
token GitHub *classic* qui a le droit `read:packages` :

```powershell
docker login ghcr.io -u <son-pseudo-github>
```

Une seule fois : copier `deploy/env.example` en `deploy/.env.qa`, puis y mettre
`HTTP_PORT=8090` et `TAG=develop`.

À chaque fois qu'elle veut tester la dernière version de `develop` (PowerShell, depuis la racine du dépôt) :

```powershell
docker compose -p valimbavaka-qa --env-file deploy/.env.qa -f deploy/docker-compose.yml -f deploy/docker-compose.qa.yml pull
docker compose -p valimbavaka-qa --env-file deploy/.env.qa -f deploy/docker-compose.yml -f deploy/docker-compose.qa.yml up -d
```

Puis ouvrir http://localhost:8090. Pour repartir d'une base vide : même commande avec `down -v`.

## Images sur GHCR

Le stockage et la bande passante du Container Registry (ghcr.io) sont gratuits pour le moment,
et ne comptent pas dans les 0,5 Go de packages du plan gratuit. GitHub annonce prévenir un mois
avant tout changement. Par précaution, `images.yml` supprime quand même les versions sans tag
(les anciennes images `:develop` remplacées) et ne crée ni tag par commit, ni attestation de provenance.

## Consommation des minutes GitHub Actions

Dépôt privé, plan gratuit : 2 000 minutes par mois sur les runners GitHub. Le déploiement sur
l'EliteBook (runner self-hosted) ne consomme rien. Ce qui consomme : la CI des PR (~5 min),
la construction des images (~5 à 8 min par merge sur `develop` ou par release), et le workflow
de synchronisation des dates du tableau (planifié toutes les 30 minutes).
