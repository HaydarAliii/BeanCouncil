# --- Build aşaması ---
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Çalışma zamanı aşaması: statik dosyaları nginx ile sun + /api'yi backend'e proxy'le ---
FROM nginx:alpine

# auth_basic için .htpasswd dosyasını container başlarken üretmek için (bkz. 40-generate-htpasswd.sh)
RUN apk add --no-cache apache2-utils

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.d/40-generate-htpasswd.sh /docker-entrypoint.d/40-generate-htpasswd.sh
RUN chmod +x /docker-entrypoint.d/40-generate-htpasswd.sh

EXPOSE 80
