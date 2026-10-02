# Make settings - @see https://tech.davis-hansson.com/p/make/
SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

# Log levels
DEBUG := $(shell printf "\e[2D\e[35m")
INFO  := $(shell printf "\e[2D\e[36m🔵 ")
OK    := $(shell printf "\e[2D\e[32m🟢 ")
WARN  := $(shell printf "\e[2D\e[33m🟡 ")
ERROR := $(shell printf "\e[2D\e[31m🔴 ")
END   := $(shell printf "\e[0m")


.PHONY: default
default: help

## help - Print help message.
.PHONY: help
help: Makefile
	@echo "usage: make <target>"
	@sed -n 's/^##//p' $<

.PHONY: build
build: test-data
	./gradlew assemble

## test-data
testDataDir := src/test/resources/shared
tempDir := ${testDataDir}/temp
gitDataDir := ${tempDir}/sdk-test-data
branchName := main
testDataRef ?= $(branchName)
githubRepoLink := https://github.com/Eppo-exp/sdk-test-data.git
.PHONY: test-data
test-data:
	rm -rf $(testDataDir)
	mkdir -p ${tempDir}
	git clone -b ${branchName} --depth 1 --single-branch ${githubRepoLink} ${gitDataDir}
	git -C ${gitDataDir} fetch --depth 1 origin "$(testDataRef)"
	git -C ${gitDataDir} checkout --detach FETCH_HEAD
	cp -r ${gitDataDir}/ufc ${testDataDir}
	rm -f ${testDataDir}/ufc/bandit-tests/*.dynamic-typing.json || true
	rm -rf ${tempDir}

.PHONY: test
test: test-data build
	./gradlew check --no-daemon

## snapshot-release - Push current branch to snapshot/<branch> to trigger snapshot publish workflow.
.PHONY: snapshot-release
snapshot-release:
	$(eval LOCAL_BRANCH := $(shell git rev-parse --abbrev-ref HEAD))
	@if [ "$(LOCAL_BRANCH)" = "HEAD" ]; then \
	  echo "Error: detached HEAD state — checkout a named branch before running snapshot-release"; \
	  exit 1; \
	fi
	@echo "$(INFO)Pushing $(LOCAL_BRANCH) to snapshot/$(LOCAL_BRANCH)$(END)"
	git push origin HEAD:refs/heads/snapshot/$(LOCAL_BRANCH)
	@echo "$(OK)Snapshot workflow triggered for snapshot/$(LOCAL_BRANCH)$(END)"

## release-plan - Validate the committed versions and show packages to publish/reuse.
.PHONY: release-plan
release-plan:
	python3 scripts/release/release.py plan

## release-draft - Prepare a draft GitHub release; publish it in GitHub to release packages.
.PHONY: release-draft
release-draft:
	python3 scripts/release/release.py draft

## test-release - Test the release planner and publication recovery behavior.
.PHONY: test-release
test-release:
	python3 -m unittest discover -s scripts/release/tests -v
