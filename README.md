# SysML v2 Pilot Ground Truth

This repository provides a small, stable entry point to the official SysML v2 Pilot parser. It keeps the official Pilot implementation as a pinned Git submodule and adds a Java API plus a command-line interface for parsing complete SysML documents or individual official grammar entries.

The parser itself is the official Xtext/EMF Pilot parser. This project does not translate or replace the grammar. The JSON tree is a serialization of the official Xtext node model, including source offsets, text, hidden tokens, and grammar elements. That makes the output useful as a ground truth for downstream parser work.

## Requirements

- Java 21 or newer
- Internet access for the first build (Maven downloads dependencies and builds the pinned Pilot)
- Git submodules initialized

## Build

```bash
git clone https://github.com/HansBug/sysml-v2-pilot-gt.git
cd sysml-v2-pilot-gt
git submodule update --init --recursive
make build
```

The resulting executable is:

```text
target/sysml-v2-pilot-gt-0.1.0-SNAPSHOT-all.jar
```

The build first compiles the exact Pilot commit recorded by the submodule, explicitly builds the upstream model and logic prerequisites, installs the resulting runtime in the local Maven repository, and then compiles this thin wrapper. The two prerequisite builds are intentional: the upstream Tycho reactor does not propagate ordinary Maven JARs through `-am`. The official runtime is not silently fetched from an unrelated version.

## CLI

Show help and provenance (`-v` is the lowercase shorthand; `-V` and `--version` are also provided by Picocli):

```bash
java -jar target/*-all.jar --help
java -jar target/*-all.jar -v
java -jar target/*-all.jar --version
```

Parse a complete document. The default entry is `rootNamespace`:

```bash
echo 'package Demo { }' | java -jar target/*-all.jar parse --format json
```

Parse a fragment using an official grammar node. The default tree is the
compact `minimal` projection:

```bash
java -jar target/*-all.jar parse --node stateDefinition --text 'state def S;'
java -jar target/*-all.jar parse --node ownedExpression --text '1 + 2 * 3' --format pretty
```

Use `--tree full` when the complete official Xtext NodeModel hierarchy is
needed for exact structural inspection:

```bash
java -jar target/*-all.jar parse --node ownedExpression --text '1 + 2 * 3' --tree full --format json
java -jar target/*-all.jar parse --node ownedExpression --file expression.txt --tree full --format semantic-json
```

List available parser entries and their official model type:

```bash
java -jar target/*-all.jar list
java -jar target/*-all.jar list --format json
```

Output formats are `pretty`, `json`, and `semantic-json`:

- `minimal` (the default `--tree` mode) removes same-range wrapper chains while preserving every range-changing child, leaf, hidden token, offset, and grammar element. It is intended for humans and LLMs.
- `full` preserves every official Xtext NodeModel composite and leaf node.
- `pretty` prints the selected tree mode for humans.
- `json` emits the selected NodeModel tree as a stable, machine-friendly object with `treeMode`, source offsets, hidden tokens, grammar elements, and diagnostics.
- `semantic-json` includes the NodeModel plus a JSON projection of the official EMF semantic AST (`type`, attributes, containment children, and non-containment references). Entries that produce no EMF object have `semantic: null`.

Syntax diagnostics go to stderr in pretty mode and are included in JSON output. All parse modes preserve the official parser's acceptance and recovery behavior; this wrapper does not add validation or linking.

Exit codes are `0` for a valid parse, `1` for syntax errors, `2` for invalid CLI usage, and `3` for a build/runtime failure.

## Java API

Add the shaded jar to a Java application and use the same official entry points:

```java
import io.github.hansbug.sysmlgt.api.NodeTree;
import io.github.hansbug.sysmlgt.api.PilotParser;
import io.github.hansbug.sysmlgt.api.SemanticTree;

PilotParser parser = PilotParser.create();
PilotParser.ParseResult result = parser.parse("state def S;", "stateDefinition");
NodeTree.TreeNode tree = NodeTree.from(result);
System.out.println(tree);

// The default is compact; request every official NodeModel layer when needed.
NodeTree.TreeNode fullTree = NodeTree.fullFrom(result);
var sameFullTree = NodeTree.from(result, NodeTree.Mode.FULL);

// The original official Xtext/EMF objects remain available for exact GT use.
var semantic = SemanticTree.from(result);
var officialRoot = result.semanticRoot();
```

`ParseResult.officialResult()` exposes the original Xtext `IParseResult`; `rootNode()` exposes the official `ICompositeNode`; and `semanticRoot()` exposes the official EMF root when available.

`PilotParser.create()` also installs the upstream `SysMLLogicStandaloneSetup`, which is required by the generated model for operator expressions and other derived features in a standalone JVM.

## Maven Packages

Each tagged release publishes both this wrapper and the matching
`org.omg.sysml:official-pilot-runtime` built from the pinned submodule to
GitHub Packages. Configure a GitHub Packages server with a token that has
`read:packages`, then add the repository and dependency:

The server id must be `github` (the release workflow uses the same id):

```xml
<server>
  <id>github</id>
  <username>YOUR_GITHUB_USERNAME</username>
  <password>YOUR_GITHUB_TOKEN</password>
</server>
```

Place that entry under `<servers>` in `~/.m2/settings.xml`, preferably using
a token stored by your normal credential helper rather than committing it.

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/HansBug/sysml-v2-pilot-gt</url>
  </repository>
</repositories>

<dependency>
  <groupId>io.github.hansbug</groupId>
  <artifactId>sysml-v2-pilot-gt</artifactId>
  <version>0.1.0</version>
</dependency>
```

The default Maven artifact is the Java API plus its matching official runtime
dependency. The executable shaded artifact is also published with classifier
`all`; use the standalone JAR or the CLI examples above when no Maven runtime
is available. Maven publishing is wired into the tag-based GitHub Actions
release workflow.

## Docker / GHCR

The image is based on Alpine Java 21 and contains only the executable shaded
JAR. Build it locally after `make build`:

```bash
make docker-build
printf 'package Demo { }\n' | docker run --rm -i \
  ghcr.io/hansbug/sysml-v2-pilot-gt:local parse --format json
```

Tagged releases publish `ghcr.io/hansbug/sysml-v2-pilot-gt:<version>` and
`ghcr.io/hansbug/sysml-v2-pilot-gt:latest`. The `docker-run` Make target also
accepts an `ARGS` string for local image checks.

## Provenance

`--version` reports this wrapper version and commit together with the pinned Pilot commit, commit time, Pilot revision, metamodel tag, and build tag. Keep the submodule pointer unchanged when reproducing a ground-truth result.

## Scope

This repository is intentionally a thin, convenient official-Pilot adapter. It does not own an alternative grammar, a semantic checker, or a downstream parser comparison suite.

## License

The wrapper code and this repository are distributed under the Eclipse Public License 2.0, aligned with the official Pilot repository. The `upstream/SysML-v2-Pilot-Implementation` submodule remains under its upstream EPL-2.0 terms.
