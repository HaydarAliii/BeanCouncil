#!/bin/sh
# nginx'in resmi image'ı /docker-entrypoint.d/ altındaki her çalıştırılabilir .sh'yi
# nginx başlamadan ÖNCE otomatik çalıştırır. Burada, APP_AUTH_USERNAME/APP_AUTH_PASSWORD'den
# nginx.conf'un auth_basic_user_file olarak kullandığı .htpasswd dosyasını üretiyoruz —
# şifre hiçbir zaman image'a/git'e gömülmez, sadece container başlarken env'den üretilir.
set -e

if [ -z "$APP_AUTH_USERNAME" ] || [ -z "$APP_AUTH_PASSWORD" ]; then
    echo "HATA: APP_AUTH_USERNAME / APP_AUTH_PASSWORD tanımlı değil — uygulama internete açık olacağından şifre koruması zorunlu." >&2
    exit 1
fi

htpasswd -cb /etc/nginx/.htpasswd "$APP_AUTH_USERNAME" "$APP_AUTH_PASSWORD"
