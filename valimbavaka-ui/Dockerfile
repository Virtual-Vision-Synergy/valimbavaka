# syntax=docker/dockerfile:1
# Image frontend : build Angular sur les runners GitHub, puis nginx sert les fichiers statiques
# et relaie /api/ vers le backend (meme origine : pas de CORS a gerer en staging/prod).

# --- Etape 1 : build Angular ---
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npx ng build --configuration production

# --- Etape 2 : nginx ---
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/valimbavaka-ui/browser /usr/share/nginx/html

EXPOSE 80
# 127.0.0.1 et non localhost : dans alpine, localhost resout d'abord en ::1 alors que nginx
# n'ecoute qu'en IPv4 (listen 80) -> healthcheck toujours "unhealthy".
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1
