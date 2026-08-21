# SysML v2 Pilot Ground Truth

This repository provides a small, stable entry point to the official SysML v2 Pilot parser. It keeps the official Pilot implementation as a pinned Git submodule and adds a Java API plus a command-line interface for parsing complete SysML documents or individual official grammar entries.

The parser itself is the official Xtext/EMF Pilot parser. This project does not translate or replace the grammar. The JSON tree is a serialization of the official Xtext node model, including source offsets, text, hidden tokens, and grammar elements. That makes the output useful as a ground truth for downstream parser work.

## Requirements

- Java 21 or newer
- Internet access for the first build (Maven downloads dependencies and builds the pinned Pilot)
- Git submodules initialized

## Build

```bash
git clone https://github.com/HansBug/sysml-v2-pilot-groundtruth.git
cd sysml-v2-pilot-groundtruth
git submodule update --init --recursive
make build
```

The resulting executable is:

```text
target/sysml-v2-pilot-groundtruth-0.1.0-SNAPSHOT-all.jar
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

Parse a fragment using an official grammar node:

```bash
java -jar target/*-all.jar parse --node stateDefinition --text 'state def S;'
java -jar target/*-all.jar parse --node ownedExpression --file expression.txt --format json
```

List available parser entries and their official model type:

```bash
java -jar target/*-all.jar list
java -jar target/*-all.jar list --format json
```

Output formats are `pretty`, `json`, and `semantic-json`:

- `pretty` prints an indented official Xtext NodeModel tree for humans.
- `json` emits that same NodeModel tree as a stable, machine-friendly object with source offsets, hidden tokens, grammar elements, and diagnostics.
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

// The original official Xtext/EMF objects remain available for exact GT use.
var semantic = SemanticTree.from(result);
var officialRoot = result.semanticRoot();
```

`ParseResult.officialResult()` exposes the original Xtext `IParseResult`; `rootNode()` exposes the official `ICompositeNode`; and `semanticRoot()` exposes the official EMF root when available.

`PilotParser.create()` also installs the upstream `SysMLLogicStandaloneSetup`, which is required by the generated model for operator expressions and other derived features in a standalone JVM.

## Provenance

`--version` reports this wrapper version and commit together with the pinned Pilot commit, commit time, Pilot revision, metamodel tag, and build tag. Keep the submodule pointer unchanged when reproducing a ground-truth result.

## Scope

This repository is intentionally a thin, convenient official-Pilot adapter. It does not own an alternative grammar, a semantic checker, or a downstream parser comparison suite.

## License

The wrapper code and this repository are distributed under the Eclipse Public License 2.0, aligned with the official Pilot repository. The `upstream/SysML-v2-Pilot-Implementation` submodule remains under its upstream EPL-2.0 terms.
