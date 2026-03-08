FROM node:24-alpine AS build
WORKDIR /app
ENV CI=1

# Copy dependency files first for layer caching
COPY client/package.json client/package-lock.json ./
RUN npm ci

# Copy source and build
COPY client/ ./
RUN npm run build

FROM nginx:stable-alpine

# Install runtime libraries required by the node binary
RUN apk add --no-cache libstdc++ libgcc

COPY --from=build /usr/local/bin/node /usr/local/bin/node

COPY --from=build /app/build /usr/share/nginx/html
COPY client/nginx/nginx.conf /etc/nginx/conf.d/default.conf

WORKDIR /usr/share/nginx/html

EXPOSE 80

CMD ["/bin/sh", "-c", "node generate-runtime-env.js && nginx -g \"daemon off;\""]
