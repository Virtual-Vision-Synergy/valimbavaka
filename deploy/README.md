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
        └─► après validation du client, Randy lance « Deployer » à la main ──► PROD sur l'EliteBook
```

| Environnement | Qui teste | Mis à jour | Où | Adresse |
|---|---|---|---|---|
| QA | Njara | à chaque merge sur `develop` (elle tire les images) | poste de Njara | http://localhost:8090 |
| Staging (UAT) | le client | à chaque release, puis figé | EliteBook | http://localhost:8081 |
| Prod | usage réel | à la main, par Randy, après validation du client | EliteBook | http://localhost |

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
| `deploy.yml` | appelé par `images.yml` (staging), ou à la main (prod, retour arrière) | déploie une version précise sur `staging` ou `prod` via le runner `workstation` |

### Pourquoi pas les « Environments » GitHub avec un délai d'attente ?

Sur un dépôt **privé** avec le plan gratuit, GitHub ne fournit ni les délais d'attente
(wait timer), ni les validations obligatoires, ni les secrets d'environnement. La mise en prod
n'est donc jamais automatique : Randy la lance à la main (voir « Mettre en prod »), et les mots de
passe restent dans les fichiers `.env.staging` / `.env.prod` de l'EliteBook, jamais sur GitHub.

## Numérotation des versions

| Version | Quand | Exemple |
|---|---|---|
| `v0.x.0` | livraisons intermédiaires avant la première mise en prod | `v0.17.0` |
| `v1.0.0` | première version validée par le client, mise en prod | `v1.0.0` |
| `v1.0.x` | correction d'un problème trouvé par le client, sans nouvelle fonction | `v1.0.1`, `v1.0.2` |
| `v1.x.0` | nouvelles fonctions (jalon suivant) | `v1.1.0` |

Le tag commence toujours par `v` : `deploy.yml` et `deploy.sh` l'attendent tel quel.

## Publier une release (Randy)

1. Njara a validé en QA tout ce qui est dans `develop`.
2. GitHub → Releases → *Draft a new release* → tag `vX.Y.Z` sur le commit validé de `develop`
   → **Publish release**. En ligne de commande :
   `gh release create v1.0.0 --target develop --title "v1.0.0" --notes-file notes.md`.
3. Le workflow « Images Docker » relance la CI, construit les images `:vX.Y.Z`, puis déploie le
   staging (onglet Actions).
4. Prévenir le client : il teste sur http://localhost:8081.

**Une release à la fois.** Les déploiements d'un même environnement passent un par un (groupe
`deploy-staging`). Si plusieurs releases sont publiées coup sur coup, GitHub ne garde qu'un
déploiement en attente : les déploiements intermédiaires sont annulés (leurs images existent
quand même sur GHCR). Attendre que le staging soit à jour avant de publier la suivante, ou
relancer à la main : `gh workflow run deploy.yml -f environment=staging -f tag=vX.Y.Z`.

## Mettre en prod (Randy, à la main)

Rien ne part en prod tout seul. Quand le client a validé la version qui tourne en staging :

- Actions → **Deployer** → *Run workflow* → environnement `prod` + version (ex. `v1.0.0`) ;
- ou, en ligne de commande : `gh workflow run deploy.yml -f environment=prod -f tag=v1.0.0`.

Le client a trouvé un problème ? Ne pas lancer la mise en prod : corriger, publier une nouvelle
release (`v1.0.1`), la faire valider en staging, puis la mettre en prod.

Avant de lancer :

- l'EliteBook est allumé et connecté (voir « Le déploiement reste en attente ») ;
- la version visée est bien celle que le client a validée en staging
  (`cat ~/valimbavaka/state/staging.tag` sur l'EliteBook) ;
- le client ne saisit rien pendant la mise à jour : le backend redémarre (1 à 2 minutes).

Suivre le déploiement depuis son poste :

```bash
gh run list --workflow deploy.yml --limit 5
gh run watch <id>            # suit le déploiement en direct
gh run view <id> --log       # journal complet, après coup
```

Ce que fait le script (`deploy/deploy.sh`), dans l'ordre : sauvegarde de la base (prod
seulement), téléchargement des images, redémarrage, attente du « UP » sur `/actuator/health`
(4 minutes au plus), puis écriture de la version dans `~/valimbavaka/state/prod.tag`. En cas
d'échec, il affiche les 80 dernières lignes du backend et revient à la version précédente.

## Retour arrière

Actions → **Deployer** → *Run workflow* → environnement + version précédente (ex. `v0.2.0`).

Le script revient aussi **tout seul** à la version précédente si le backend d'une nouvelle
version ne répond pas « UP » sur `/actuator/health` dans les 4 minutes.

Revenir à une image précédente ne touche pas la base : les changesets Liquibase déjà appliqués
restent. Avec des changesets additifs (nouvelles tables ou colonnes), l'ancienne version
fonctionne quand même, car `ddl-auto=validate` accepte les tables et colonnes en plus. Si une
release a supprimé ou renommé quelque chose, il faut aussi restaurer une sauvegarde.

Le même workflow sert à la mise en prod (voir « Mettre en prod »).

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

## Savoir ce qui tourne, lire les journaux

Sur l'EliteBook (WSL, compte `vvs`) :

```bash
cat ~/valimbavaka/state/staging.tag ~/valimbavaka/state/prod.tag   # versions déployées
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'     # conteneurs et leur état
docker logs --tail 100 valimbavaka-prod-core-1                     # journal du backend (prod)
docker logs --tail 20 valimbavaka-prod-backup-1                    # dernières sauvegardes
ls -lt ~/valimbavaka/backups/prod | head                           # fichiers de sauvegarde
```

Pour le staging, remplacer `prod` par `staging` dans le nom du conteneur. Les fichiers
`state/<env>.tag` sont écrits par `deploy.sh` après un déploiement réussi : c'est d'eux que
dépend le retour arrière automatique. Ne pas les modifier à la main.

## Dépannage

### Le déploiement reste en attente (« Queued », « Waiting for a runner »)

Le runner de l'EliteBook est hors ligne. Vérifier depuis son poste :

```bash
gh api repos/Virtual-Vision-Synergy/valimbavaka/actions/runners --jq '.runners[] | "\(.name) \(.status)"'
```

Si `elitebook` est `offline`, dans l'ordre :

1. **Le poste est-il allumé et connecté ?** Le Wi-Fi ne se reconnecte pas tout seul après un
   redémarrage : il faut le reconnecter à la main (sur place, ou en bureau à distance via Tailscale).
2. **WSL tourne-t-il ?** Ouvrir un terminal Ubuntu suffit à le relancer.
3. **Le service du runner tourne-t-il ?** Dans le dossier du runner : `sudo ./svc.sh status`,
   puis `sudo ./svc.sh start` si besoin.

Dès que le runner revient, GitHub lui envoie le déploiement en attente : rien à relancer.
Un déploiement en attente depuis plus de 24 h est abandonné par GitHub ; le relancer avec
`gh workflow run deploy.yml -f environment=<env> -f tag=<version>`.

Annuler un déploiement en attente dont on ne veut plus : Actions → ouvrir l'exécution →
*Cancel workflow*, ou `gh run cancel <id>`.

### Le déploiement échoue

| Message | Cause | Quoi faire |
|---|---|---|
| `Fichier introuvable : …/.env.<env>` | fichier d'environnement absent | le créer depuis `deploy/env.example` |
| `… manquant` (`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `HTTP_PORT`, `BACKUP_DIR`…) | variable obligatoire vide | la remplir (voir « Variables d'environnement ») |
| échec au téléchargement des images | pas d'internet, ou `docker login ghcr.io` expiré | reconnecter, refaire `docker login ghcr.io` |
| `ECHEC : le backend … ne repond pas` | le backend ne démarre pas | lire les logs affichés juste en dessous ; la version précédente a été remise |
| sauvegarde en échec (prod) | base arrêtée ou disque plein | rien n'a été touché ; corriger puis relancer |

Causes fréquentes d'un backend qui ne démarre pas : `JWT_SECRET` de moins de 32 caractères,
`ADMIN_PASSWORD` de moins de 15 caractères sur une base neuve, migration Liquibase en échec,
mémoire insuffisante (`CORE_MEM_LIMIT`).

### L'application répond mais on ne peut pas rester connecté

Session perdue à chaque rechargement de page, ou erreur 403 au rafraîchissement :

- `CORS_ALLOWED_ORIGINS` doit être l'adresse exacte tapée dans le navigateur, schéma et port
  compris (`http://localhost:8081` n'est pas `http://192.168.1.20:8081`) ;
- en HTTP (pas de HTTPS), `REFRESH_COOKIE_SECURE=false` est obligatoire, sinon le navigateur
  refuse le cookie de session.

Après une modification d'un fichier `.env.<env>`, relancer le déploiement de la même version
pour qu'elle soit prise en compte.

## Variables d'environnement

Fichiers `~/valimbavaka/.env.staging` et `~/valimbavaka/.env.prod` sur l'EliteBook, `deploy/.env.qa`
chez Njara. Modèle : `deploy/env.example`. Jamais commités, jamais sur GitHub, `chmod 600`.

| Variable | Obligatoire | Rôle |
|---|---|---|
| `SPRING_PROFILE` | non (`staging`) | `staging` ou `prod` ; la QA utilise `staging` |
| `HTTP_PORT` | oui | port de l'application : staging `8081`, prod `80`, QA `8090` |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | oui | base PostgreSQL ; mot de passe différent par environnement |
| `JWT_SECRET` | oui | signature des jetons, 32 caractères au moins (`openssl rand -base64 48`). Le changer déconnecte tout le monde |
| `CORS_ALLOWED_ORIGINS` | oui | adresse(s) exacte(s) de l'application dans le navigateur, séparées par des virgules, jamais `*` |
| `REFRESH_COOKIE_SECURE` | non (`true`) | `false` seulement si l'environnement est servi en HTTP |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_FIRST_NAME`, `ADMIN_LAST_NAME` | au premier démarrage | premier administrateur, créé seulement sur une base sans administrateur ; mot de passe provisoire de 15 caractères au moins, à changer à la première connexion |
| `INVOICE_ISSUER_NIF` | non | NIF de l'émetteur imprimé sur les factures ; vide = pas de NIF sur la facture |
| `CORE_MEM_LIMIT`, `DB_MEM_LIMIT` | non (`1g`, `512m`) | plafonds mémoire ; en prod, `CORE_MEM_LIMIT=2g` |
| `BACKUP_DIR` | prod | dossier des sauvegardes, chemin absolu |
| `BACKUP_INTERVAL_SECONDS`, `BACKUP_KEEP` | non (`1800`, `48`) | rythme et nombre des sauvegardes automatiques (prod) |
| `DB_PUBLISHED_PORT` | non (staging `5433`, QA `5434`) | port local de PostgreSQL pour DBeaver ; en prod, la base n'est publiée nulle part |
| `TAG` | QA seulement (`develop`) | version des images ; en staging et prod, `deploy.sh` la fournit |

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
