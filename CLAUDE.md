# Developer Guide

## Mission

`sysml-v2-pilot-gt` is a thin, reproducible wrapper around the official OMG
SysML v2 Pilot implementation. Its job is to provide a convenient Java API
and CLI that expose the official parser's result as a ground truth (GT) for
downstream SysML tooling.

The repository is a reference and comparison aid, not a production parser or
a core application service. A downstream grammar or parser may use this
project to parse the same input and compare results, but that downstream work
does not belong here.

## Design Principles

- The pinned official Pilot parser is the authority for acceptance, recovery,
  grammar entry names, diagnostics, source ranges, and semantic objects.
- The wrapper should add ergonomics, not language behavior. Do not change,
  normalize, validate, link, or reinterpret official parse results.
- Keep the public surface small: a stable Java API, a script-friendly Picocli
  CLI, and reproducible build metadata.
- Preserve enough information for exact investigations: offsets, end offsets,
  source text, hidden tokens, grammar elements, diagnostics, and the pinned
  Pilot commit.
- The default minimal tree is only a presentation projection that folds
  redundant same-range Xtext wrappers. `NodeTree.fullFrom` and `Mode.FULL`
  remain available when the complete official NodeModel is required.

## Explicit Non-Goals

Do not add any of the following to this repository:

- An ANTLR grammar, ANTLR parser, or ANTLR runtime integration.
- Differential-test orchestration against another parser. That is a downstream
  consumer responsibility.
- A copied or modified fork of the official grammar.
- A replacement semantic model, semantic checker, formatter, linker, or
  production service layer.
- Large framework abstractions when a small Maven, Java, or Picocli change is
  sufficient.

If a requested feature would make this wrapper the owner of SysML language
behavior, stop and reassess it against this boundary.

## Repository Map

### Official dependency

`upstream/SysML-v2-Pilot-Implementation` is a Git submodule. Its recorded
commit is part of every GT result. Update the submodule pointer deliberately
and review the upstream license and release metadata before changing it.

### Build and packaging

- `scripts/build-official.sh` builds the pinned upstream runtime and records
  the Pilot commit, commit time, revision, metamodel tag, and build tag under
  `target/`.
- `build.sh [VERSION]` builds the official runtime first, installs it with the
  requested matching Maven version, then runs this wrapper's Maven `verify`.
  The default development version is `0.1.0-SNAPSHOT`.
- `pom.xml` defines the Java 21 wrapper, API dependencies, shaded CLI JAR,
  source JAR, Maven publication metadata, and JaCoCo report generation.
- `scripts/official-runtime-pom.xml` is the publication POM for the matching
  official runtime artifact. `scripts/publish-maven.sh VERSION` publishes the
  runtime and wrapper together.
- `Makefile` is the short command interface. Run `make help` before inventing
  a new local command.

### Wrapper code

- `api/PilotParser.java` initializes the official standalone setup, discovers
  official parser entry points, parses text, and returns the original Xtext
  parse result plus diagnostics and semantic root.
- `api/NodeTree.java` serializes the official Xtext NodeModel. `MINIMAL` is a
  compact view; `FULL` is the complete node hierarchy.
- `api/SemanticTree.java` provides a JSON-friendly view of the official EMF
  semantic object graph while keeping the original object available through
  `ParseResult.semanticRoot()`.
- `api/BuildInfo.java` exposes wrapper and upstream provenance.
- `cli/Main.java` provides `parse`, `list`, `-v/--version`, pretty output,
  JSON output, semantic JSON, source text/file/stdin input, and tree mode
  selection. Keep CLI errors and exit codes stable.

### Tests and automation

- `src/test/java` contains focused tests for the API, tree projections,
  semantic projection, provenance, and CLI behavior.
- `.github/workflows/test.yml` builds the pinned Pilot, runs wrapper tests and
  JaCoCo, and uploads Surefire and coverage reports for pushes and pull
  requests.
- `.github/workflows/release.yml` runs for `v*` tags (or an explicit manual
  version), builds the same pinned runtime, uploads the shaded JAR to a GitHub
  Release, and publishes the matching Maven and GHCR artifacts.
- `Dockerfile` is only a small Alpine Java 21 runtime image around the shaded
  JAR. It is not an alternate distribution of the parser logic.
- `AGENTS.md` is intentionally a symlink to this file so all coding agents
  receive the same instructions.

## Normal Development

From a fresh checkout:

```bash
git submodule update --init --recursive
make build
```

Useful commands:

```bash
make help                         # list the supported commands
make test                         # build and run unit tests
make verify                       # build, test, and generate JaCoCo
make clean                        # remove Maven output
make docker-build                 # build the local CLI image
make docker-run ARGS="--help"    # run the local image
make release VERSION=0.1.0        # build an explicit release version
```

`./mvnw verify` is appropriate after the official runtime has already been
installed by `build.sh`. On a clean checkout, use `./build.sh` or `make verify`
so the pinned upstream runtime exists first. The first build needs
network access and can take several minutes because the upstream Tycho reactor
is compiled.

## Ground-Truth Review Rules

When changing parser-facing code, verify all of the following:

1. The same input and official grammar node still produce the same acceptance
   and diagnostics.
2. `ParseResult.officialResult()`, `rootNode()`, and `semanticRoot()` remain
   usable for exact downstream inspection.
3. JSON retains source offsets, text, hidden leaves, grammar element names,
   diagnostics, and an explicit schema/tree mode marker.
4. The minimal projection only removes redundant same-range wrappers; use the
   full projection for structural fidelity.
5. `--version` still identifies both this wrapper commit/version and the exact
   upstream Pilot commit/version metadata.
6. Tests and README examples cover every changed public behavior.

Do not silently fix an upstream parse result in wrapper code. If official
behavior changes, update the submodule pointer and provenance deliberately,
then document the change.

## Release and Contribution Checklist

Before committing a change:

```bash
git diff --check
./build.sh
actionlint .github/workflows/*.yml
git -C upstream/SysML-v2-Pilot-Implementation status --short
```

Confirm that the upstream submodule is clean, no generated `target/` files are
tracked, and no ANTLR or differential-test code was introduced. Keep commits
small and explain public behavior changes in `README.md`. Release tags use
`v<version>`; the release workflow derives the Maven and container version
from that tag and attaches `target/*-all.jar` to the GitHub Release.

## License

The wrapper repository follows the Eclipse Public License 2.0, aligned with
the official Pilot. The submodule remains governed by its upstream license and
not by any attempt to relicense or copy its source here.
