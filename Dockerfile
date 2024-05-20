FROM eclipse-temurin:21.0.2_13-jre-jammy AS build

WORKDIR /app
# System deps
ENV PATH="/root/.local/bin:/root/.local/share/mise/shims:$PATH"
RUN apt update && apt install git -y && curl https://mise.run | sh
COPY .tool-versions /app/
RUN mise install -y node clojure

# Node deps
COPY package.json package-lock.json /app/
RUN npm i

# Clojure deps
COPY deps.edn  /app/
RUN clojure -P -X:cljs:shadow

# Build ui and uberjar
COPY . /app
RUN npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/output-prod.css --minify \
    && clojure -M:dev:cljs:shadow release app \
    && clojure -T:build build

# Result image
FROM eclipse-temurin:21.0.2_13-jre-jammy
LABEL org.opencontainers.image.source=https://github.com/abogoyavlensky/clojure-kamal-example

WORKDIR /app
COPY --from=build /app/target/standalone.jar /app/standalone.jar

# Run application
EXPOSE 80
CMD ["java", "-Xms64m", "-Xmx256m", "-jar", "standalone.jar"]
