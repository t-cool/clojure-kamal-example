# Full-stack Clojure/Script app with deployment using Kamal

This project demonstrates the setup of a Clojure/Script web application that uses PostgreSQL as its database.
It also includes configuration for deploying the app with Kamal on **a single server**.

Key backend libs:
- Integrant
- Reitit
- Malli
- next.jdbc
- HoneySQL
- Automigrate

Key frontend libs:
- re-frame
- Reitit
- Shadow CLJS
- TailwindCSS

Other tools:
- Kamal
- GitHub Actions
- mise-en-place
- Taskfile
- Testcontainers

## The app

This setup provides a Clojure/Script web application with an example API route for fetching 
a list of movies and displaying them on the main page.

![App main page](/docs/app_page.png)

## Deploy: summary

You need to have Docker installed on your local machine.
Additionally, you need an empty server that you can access using SSH keys.
First deployment: 

```shell
./kamal.sh envify --skip-push  # :warning: then fill all variables in the newly created `.env` file
./kamal.sh server bootstrap
ssh root@192.168.0.1 'docker network create traefik'
ssh root@192.168.0.1 'mkdir -p /root/letsencrypt && touch /root/letsencrypt/acme.json && chmod 600 /root/letsencrypt/acme.json'
./kamal.sh setup
./kamal.sh app exec 'java -jar standalone.jar migrations'
```

For subsequent deployments from the local machine:

```shell
./kamal.sh deploy
```

or push to the master branch.

## Deploy: step-by-step

### Requirements

Ensure you have Docker [installed](https://docs.docker.com/engine/install/) on your machine.

We will use a predefined command to run a dockerized version of Kamal,
so you don’t need to install anything else to deploy your app.

```shell
./kamal.sh version
```

---

:information_source: **Note**: _Alternatively you can install Kamal as Ruby gem 
and use the `kamal` command instead of dockerized version:_

Install [mise-en-place](https://mise.jdx.dev/getting-started.html#quickstart) (or [asdf](https://asdf-vm.com/guide/getting-started.html)), 
add `ruby 3.3.0` to `.tool-versions` file and run:

```shell
brew install libyaml  # or on Ubuntu: `sudo apt-get install libyaml-dev` 
mise install ruby
gem install kamal -v 1.5.2
kamal version
```

### Initial server setup

#### Setup environment variables

Run command `envify` to create a `.env` with all required empty variables: 

```shell
./kamal.sh envify --skip-push
```

_The `--skip-push` parameter prevents the `.env` file from being pushed to the server._

Now, you can fill all environment variables in the .env file with actual values for deployment on the server.
Here’s an example:

```shell
# Generated by kamal envify
# DEPLOY
SERVER_IP=192.168.0.1
REGISTRY_USERNAME=your-username
REGISTRY_PASSWORD=secret-registry-password
TRAEFIK_ACME_EMAIL=your_email@example.com
APP_DOMAIN=app.domain.com

# App
DATABASE_URL="jdbc:postgresql://clojure-kamal-example-db:5432/demo?user=demoadmin&password=secret-db-password"

# DB accessory
POSTGRES_DB=demo
POSTGRES_USER=demoadmin
POSTGRES_PASSWORD=secret-db-password
```

Notes:
- `SERVER_IP` - the IP of the server you want to deploy your app, you should be able to connect to it using ssh-keys.
- `REGISTRY_USERNAME` and `REGISTRY_PASSWORD` - credentials for docker registry, in our case we are using `ghcr.io`, but it can be any registry. 
- `TRAEFIK_ACME_EMAIL` - email for register TLS-certificate with Let's Encrypt and Traefik.
- `APP_DOMAIN` - domain of your app, should be configured to point to `SERVER_IP`. 
- `clojure-kamal-example-db` - this is the name of the database container from accessories section of `deploy/config.yml` file.
- We duplicated database credentials to set up database container and use `DATABASE_URL` in the app. 

:warning: _Do not include file `.env` to git repository!_ 

#### Bootstrap server and deploy app

Install Docker on a server:

```shell
./kamal.sh server bootstrap
```

Create a Docker network for access to the database container from the app by container name
and a directory for Let’s Encrypt certificates:

```shell
ssh root@192.168.0.1 'docker network create traefik'
ssh root@192.168.0.1 'mkdir -p /root/letsencrypt && touch /root/letsencrypt/acme.json && chmod 600 /root/letsencrypt/acme.json'
```

Set up Traefik, the database, environment variables and run app on a server:

```shell
./kamal.sh setup
```

The app is deployed on the server, but it is not fully functional yet. You need to run database migrations:

```shell
./kamal.sh app exec 'java -jar standalone.jar migrations'
```

Now, the application is fully deployed on the server.

### Regular deploy

For subsequent deployments from the local machine, run:

```shell
./kamal.sh deploy
```

Or just push to the master branch, there is a GitHub Actions pipeline that does 
the deployment automatically `.github/workflows/deploy.yaml`.

### Setup CI for deployment

For CI setup you need to add following environment variables as secrets for Actions. 
In GitHub UI of the repository navigate to `Settings -> Secrets and variables -> Actions`.
Then add variables with the same values you added to local `.env` file:

```shell
APP_DOMAIN
DATABASE_URL
POSTGRES_DB
POSTGRES_PASSWORD
POSTGRES_USER
SERVER_IP
SSH_PRIVATE_KEY
TRAEFIK_ACME_EMAIL
```

#### Notes

- `SSH_PRIVATE_KEY` - a new SSH private key **without password** that you created and added public part of it to servers's `~/.ssh/authorized_keys` to authorize from CI-worker.

To generate SSH keys, run:

```shell
ssh-keygen -t ed25519 -C "your_email@example.com"
```

## Development

### System deps
Install [mise-en-place](https://mise.jdx.dev/getting-started.html#quickstart) (or [asdf](https://asdf-vm.com/guide/getting-started.html)), 
then to install system deps run:

```shell
mise install
```

### Frontend

Run frontend in watch mode (js and css):
```shell
task ui
```

### Backend

Create file `.env.local` with local database credentials, for example:

```shell
POSTGRES_DB=demo
POSTGRES_USER=demo
POSTGRES_PASSWORD=demo
DATABASE_URL=jdbc:postgresql://localhost:5432/demo?user=demo&password=demo
````

⚠️ Do not include file `.env.local` to git repository!

Run database for local development:

```shell
task up
```

Start REPL:

```shell
task repl
```

Run backend in the REPL:

```clojure
(reset)
```

Run tests:

```shell
task test
```

Manage database migrations:

```shell
task migrations -- list
task migrations -- make
task migrations -- migrate
task migrations -- explain :number 1
```

Print all available commands:

```text
task -l

task: Available tasks for this project:
* build:                Build uberjar
* check:                Run lint, fmt and tests. Intended to use locally
* css-prod:             Build css in prod mode
* deps:                 Install all dev deps
* fmt:                  Fix code formatting
* fmt-check:            Check code formatting
* lint:                 Linting project's code
* lint-init:            Linting project's classpath
* migrations:           Manage db migrations
* outdated:             Upgrade outdated Clojure deps versions
* outdated-check:       Check outdated deps versions
* repl:                 Run built-in Clojure repl
* test:                 Run tests
* ui:                   Build js and css in watch mode for local development
* up:                   Run docker services for local development
```

## License

Copyright © 2024 Andrey Bogoyavlenskiy

Distributed under the MIT License.
