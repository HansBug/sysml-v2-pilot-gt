.DEFAULT_GOAL := help

MAVEN ?= ./mvnw
VERSION ?= 0.1.0-SNAPSHOT

.PHONY: help init build test verify jar publish docker-build docker-run clean release

help: ## Show available development commands
	@printf '\033[1;36mSysML v2 Pilot Ground Truth\033[0m\n\n'
	@awk 'BEGIN {FS = ":.*## "; printf "\033[1;33mUsage:\033[0m make \033[36m<target>\033[0m [VERSION=x.y.z]\n\n\033[1;33mTargets:\033[0m\n"} /^[a-zA-Z0-9_.-]+:.*## / {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

init: ## Initialize the official Pilot submodule
	git submodule update --init --recursive

build: ## Build the pinned official Pilot and the executable shaded JAR
	./build.sh "$(VERSION)"

test: ## Run the project unit tests
	./build.sh "$(VERSION)"

verify: ## Run tests and generate the JaCoCo coverage report
	./build.sh "$(VERSION)"

jar: build ## Alias for build

publish: build ## Publish matching runtime and wrapper packages to Maven
	./scripts/publish-maven.sh "$(VERSION)"

docker-build: build ## Build a local Alpine Java 21 container image
	docker build --build-arg VERSION="$(VERSION)" -t "$${IMAGE:-ghcr.io/hansbug/sysml-v2-pilot-gt:local}" .

docker-run: ## Run the container, forwarding stdin and CLI arguments
	docker run --rm -i "$${IMAGE:-ghcr.io/hansbug/sysml-v2-pilot-gt:local}" $(ARGS)

clean: ## Remove Maven build output
	$(MAVEN) clean

release: ## Build a release version, for example: make release VERSION=0.1.0
	@test "$(VERSION)" != "0.1.0-SNAPSHOT" || (echo "set VERSION=x.y.z for a release" >&2; exit 2)
	./build.sh "$(VERSION)"
