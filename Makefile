# mini-eshop — build & run helpers.
# Compose files live in deployments/; --project-directory . keeps build contexts and .env
# resolving from this repo root. Run targets from this directory.

COMPOSE      := docker compose --project-directory . -f deployments/docker-compose.yml
COMPOSE_DEV  := $(COMPOSE) -f deployments/docker-compose.dev.yml
COMPOSE_PROD := $(COMPOSE) -f deployments/docker-compose.prod.yml

.DEFAULT_GOAL := help
.PHONY: help certs build up seed dev prod down logs ps clean

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-8s\033[0m %s\n", $$1, $$2}'

certs: ## Generate the self-signed TLS cert for nginx (run once)
	./nginx/gen-cert.sh

build: ## Build all images
	$(COMPOSE) build

up: ## Start the full stack (builds if needed), detached
	$(COMPOSE) up -d --build

seed: ## Load demo users (alice/alicepass, bob/bobpass) — run after `make up`
	$(COMPOSE) exec -T postgres sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"' < scripts/seed-users.sql

dev: ## Start the stack + dev UIs (RedisInsight, pgAdmin)
	$(COMPOSE_DEV) up -d --build

prod: ## Start the stack with the real-email (prod) profile — needs SMTP vars in .env
	$(COMPOSE_PROD) up -d --build

down: ## Stop the stack
	$(COMPOSE) down

logs: ## Tail backend logs
	$(COMPOSE) logs -f backend

ps: ## Show service status
	$(COMPOSE) ps

clean: ## Stop and remove volumes (DESTROYS Postgres + Redis data)
	$(COMPOSE) down -v
