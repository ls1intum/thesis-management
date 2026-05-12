FROM node:24.15.0-alpine AS build
WORKDIR /app
ENV CI=1
ENV HUSKY=0
RUN corepack enable

# Copy dependency files (plus patches so patch-package can run in postinstall)
# first for layer caching
COPY client/package.json client/pnpm-lock.yaml client/pnpm-workspace.yaml ./
COPY client/patches/ ./patches/
RUN pnpm install --frozen-lockfile

# Copy source and build
COPY client/ ./
RUN pnpm build

FROM nginx:stable-alpine

# Install runtime libraries required by the node binary
RUN apk add --no-cache libstdc++ libgcc

COPY --from=build /usr/local/bin/node /usr/local/bin/node

COPY --from=build /app/build /usr/share/nginx/html
COPY client/nginx/nginx.conf /etc/nginx/conf.d/default.conf

WORKDIR /usr/share/nginx/html

EXPOSE 80

CMD ["/bin/sh", "-c", "node generate-runtime-env.js && nginx -g \"daemon off;\""]
