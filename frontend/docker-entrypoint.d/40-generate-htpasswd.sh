#!/bin/sh
# nginx's official image automatically runs every executable .sh under /docker-entrypoint.d/
# BEFORE nginx starts. Here we generate the .htpasswd file that nginx.conf uses as
# auth_basic_user_file, from APP_AUTH_USERNAME/APP_AUTH_PASSWORD — the password is never
# baked into the image or committed to git, only generated from env at container start.
set -e

if [ -z "$APP_AUTH_USERNAME" ] || [ -z "$APP_AUTH_PASSWORD" ]; then
    echo "ERROR: APP_AUTH_USERNAME / APP_AUTH_PASSWORD not set — password protection is required since the app is internet-facing." >&2
    exit 1
fi

htpasswd -cb /etc/nginx/.htpasswd "$APP_AUTH_USERNAME" "$APP_AUTH_PASSWORD"
