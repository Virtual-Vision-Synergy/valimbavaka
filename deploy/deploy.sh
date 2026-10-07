#!/usr/bin/env bash
# Deploie une version sur l'EliteBook : bash deploy/deploy.sh <staging|prod> <vX.Y.Z>
#
# 1. (prod) sauvegarde de la base avant toute chose ;
# 2. tire les images de la version et relance la stack ;
# 3. attend que le backend reponde "UP" sur /actuator/health ;
# 4. si ce n'est pas le cas : revient automatiquement a la version precedente et echoue ;
# 5. sinon : memorise la version deployee et supprime les vieilles images (10 gardees).
#
# ATTENTION : revenir a une image precedente NE restaure PAS la base. Les changesets Liquibase
# deja appliques restent. D'ou la regle du projet : changesets uniquement additifs (on ajoute
# des tables/colonnes, on n'en supprime ni n'en renomme jamais dans la meme release).
# Pour restaurer les donnees elles-memes : voir deploy/README.md, "Restaurer une sauvegarde".

set -euo pipefail

ENVIRONMENT="${1:?Usage: deploy.sh <staging|prod> <vX.Y.Z>}"
TAG="${2:?Usage: deploy.sh <staging|prod> <vX.Y.Z>}"
export TAG  # docker compose l'utilise pour choisir les images (${TAG} dans docker-compose.yml)

case "$ENVIRONMENT" in staging|prod) ;; *) echo "Environnement inconnu : $ENVIRONMENT" >&2; exit 1;; esac

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VVS_HOME="${VVS_HOME:-$HOME/valimbavaka}"
ENV_FILE="$VVS_HOME/.env.$ENVIRONMENT"
STATE_DIR="$VVS_HOME/state"
STATE_FILE="$STATE_DIR/$ENVIRONMENT.tag"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-240}"
KEEP_IMAGES="${KEEP_IMAGES:-10}"

log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }

[ -f "$ENV_FILE" ] || { echo "Fichier introuvable : $ENV_FILE (modele : deploy/env.example)" >&2; exit 1; }
mkdir -p "$STATE_DIR"

compose() {
  docker compose -p "valimbavaka-$ENVIRONMENT" --env-file "$ENV_FILE" \
    -f "$DEPLOY_DIR/docker-compose.yml" -f "$DEPLOY_DIR/docker-compose.$ENVIRONMENT.yml" "$@"
}

# Charge DB_USERNAME / DB_NAME / BACKUP_DIR depuis le fichier d'environnement.
env_value() { grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d= -f2- || true; }

backup_before_deploy() {
  local backup_dir db_user db_name db_container file
  backup_dir="$(env_value BACKUP_DIR)"
  db_user="$(env_value DB_USERNAME)"
  db_name="$(env_value DB_NAME)"
  [ -n "$backup_dir" ] || { echo "BACKUP_DIR manquant dans $ENV_FILE" >&2; exit 1; }
  mkdir -p "$backup_dir"

  db_container="$(TAG="${PREVIOUS_TAG:-$TAG}" compose ps -q db 2>/dev/null || true)"
  if [ -z "$db_container" ]; then
    log "Pas de base en cours d'execution (premier deploiement) : pas de sauvegarde prealable."
    return
  fi

  file="$backup_dir/avant-$TAG-$(date +%Y%m%d-%H%M%S).sql.gz"
  log "Sauvegarde de la base avant deploiement -> $file"
  if ! docker exec "$db_container" pg_dump -U "$db_user" -d "$db_name" --no-owner | gzip > "$file.tmp"; then
    rm -f "$file.tmp"
    echo "La sauvegarde a echoue : deploiement annule, rien n'a ete modifie." >&2
    exit 1
  fi
  mv "$file.tmp" "$file"
  # Les sauvegardes "avant-*" ne sont jamais supprimees automatiquement (une par mise en prod).
}

wait_healthy() {
  local container status elapsed=0
  container="$(compose ps -q core)"
  [ -n "$container" ] || return 1
  while [ "$elapsed" -lt "$HEALTH_TIMEOUT" ]; do
    status="$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || echo absent)"
    case "$status" in
      healthy) return 0 ;;
      unhealthy) return 1 ;;
    esac
    sleep 5
    elapsed=$((elapsed + 5))
  done
  return 1
}

up_version() {
  TAG="$1" compose pull core ui
  TAG="$1" compose up -d --remove-orphans
}

prune_images() {
  local app
  for app in core ui; do
    docker images "ghcr.io/virtual-vision-synergy/valimbavaka-$app" --format '{{.Repository}}:{{.Tag}}' \
      | tail -n +"$((KEEP_IMAGES + 1))" | xargs -r docker rmi >/dev/null 2>&1 || true
  done
  docker image prune -f >/dev/null 2>&1 || true
}

PREVIOUS_TAG="$(cat "$STATE_FILE" 2>/dev/null || true)"
log "Deploiement $ENVIRONMENT : ${PREVIOUS_TAG:-aucune version} -> $TAG"

if [ "$ENVIRONMENT" = "prod" ]; then
  backup_before_deploy
fi

up_version "$TAG"

log "Attente du health check du backend (max ${HEALTH_TIMEOUT}s)..."
if wait_healthy; then
  echo "$TAG" > "$STATE_FILE"
  prune_images
  log "OK : $ENVIRONMENT tourne en $TAG."
  exit 0
fi

log "ECHEC : le backend $TAG ne repond pas. Derniers logs :"
TAG="$TAG" compose logs --tail 80 core || true

if [ -n "$PREVIOUS_TAG" ] && [ "$PREVIOUS_TAG" != "$TAG" ]; then
  log "Retour automatique a $PREVIOUS_TAG..."
  up_version "$PREVIOUS_TAG"
  if wait_healthy; then
    log "Retour arriere reussi : $ENVIRONMENT tourne de nouveau en $PREVIOUS_TAG."
  else
    log "Le retour arriere ne repond pas non plus : intervention manuelle necessaire."
  fi
else
  log "Aucune version precedente connue : pas de retour arriere possible."
fi
exit 1
