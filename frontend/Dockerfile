FROM nginx:1.27.5-alpine

# nginx settings: serve the Vue app and send API calls to the gateway
COPY nginx.conf /etc/nginx/conf.d/default.conf

# cp the built Vue app (run "npm run build" first, it creates dist/)
COPY dist /usr/share/nginx/html

# expose port 80 for nginx
EXPOSE 80
