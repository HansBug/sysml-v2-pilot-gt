#!/usr/bin/env bash
set -euo pipefail

vendor_dir=${1:?vendor directory is required}
wrapper_version=${2:-0.1.0-SNAPSHOT}
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
pilot_dir="$repo_root/upstream/SysML-v2-Pilot-Implementation"
mkdir -p "$vendor_dir"

if [[ ! -e "$pilot_dir/.git" ]]; then
  echo "official Pilot submodule is not initialized; run: git submodule update --init --recursive" >&2
  exit 2
fi

target_jar="$vendor_dir/pilot-runtime.jar"
official_commit=$(git -C "$pilot_dir" rev-parse HEAD)
needs_build=1
if [[ -f "$target_jar" && -f "$vendor_dir/pilot-commit.txt" \
      && "$(cat "$vendor_dir/pilot-commit.txt")" == "$official_commit" ]]; then
  needs_build=0
fi

if [[ "$needs_build" == "1" && "${SKIP_OFFICIAL_BUILD:-0}" != "1" ]]; then
  echo "Building official Pilot at ${official_commit:0:12}..." >&2
  # Tycho copies the plain model/logic jars into tracked bundle/lib paths.
  # Preserve those paths (including any pre-existing local edits) so a wrapper
  # build never leaves the pinned submodule dirty.
  backup_dir=$(mktemp -d "${TMPDIR:-/tmp}/sysml-pilot-build.XXXXXX")
  tracked_libs=(
    "org.omg.sysml.model.bundle/lib/org.omg.sysml.model-0.61.0-SNAPSHOT.jar"
    "org.omg.sysml.logic.bundle/lib/org.omg.sysml.logic-0.61.0-SNAPSHOT.jar"
  )
  restore_libs() {
    for relative in "${tracked_libs[@]}"; do
      if [[ -f "$backup_dir/$relative" ]]; then
        mkdir -p "$(dirname "$pilot_dir/$relative")"
        cp "$backup_dir/$relative" "$pilot_dir/$relative"
      fi
    done
    rm -rf "$backup_dir"
  }
  for relative in "${tracked_libs[@]}"; do
    if [[ -f "$pilot_dir/$relative" ]]; then
      mkdir -p "$backup_dir/$(dirname "$relative")"
      cp "$pilot_dir/$relative" "$backup_dir/$relative"
    fi
  done
  trap restore_libs EXIT
  (
    cd "$pilot_dir"
    # Tycho's -am does not carry ordinary Maven jars through its target
    # platform, so install those two upstream prerequisites explicitly first.
    ./mvnw -B -DskipTests -pl org.omg.sysml.model -am clean install
    ./mvnw -B -DskipTests -pl org.omg.sysml.logic -am clean install
    ./mvnw -B -DskipTests -pl org.omg.sysml.jupyter.kernel -am clean package
  )

  jar=$(find "$pilot_dir/org.omg.sysml.jupyter.kernel/target" -maxdepth 1 -type f -name '*-all.jar' -print -quit)
  if [[ -z "$jar" ]]; then
    echo "official Pilot build did not produce a jupyter kernel fat jar" >&2
    exit 3
  fi
  cp "$jar" "$target_jar"
elif [[ ! -f "$target_jar" ]]; then
  echo "pilot runtime is missing and SKIP_OFFICIAL_BUILD=1 was requested" >&2
  exit 4
fi
printf '%s\n' "$official_commit" > "$vendor_dir/pilot-commit.txt"
printf '%s\n' "$(git -C "$pilot_dir" show -s --format=%cI HEAD)" > "$vendor_dir/pilot-commit-time.txt"
mkdir -p "$repo_root/target/generated-resources"
cat > "$repo_root/target/generated-resources/build-info.properties" <<EOF
project.version=$wrapper_version
wrapper.commit=$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo unknown)
wrapper.commit.time=$(git -C "$repo_root" show -s --format=%cI HEAD 2>/dev/null || echo unknown)
pilot.commit=$(cat "$vendor_dir/pilot-commit.txt")
pilot.commit.time=$(cat "$vendor_dir/pilot-commit-time.txt")
pilot.revision=$(sed -n 's:.*<revision>\([^<]*\)</revision>.*:\1:p' "$pilot_dir/pom.xml" | head -1)
pilot.metamodel.tag=$(sed -n 's:.*<sysml.metamodel-tag>\([^<]*\)</sysml.metamodel-tag>.*:\1:p' "$pilot_dir/pom.xml" | head -1)
pilot.build.tag=$(sed -n 's:.*<sysml.build-tag>\([^<]*\)</sysml.build-tag>.*:\1:p' "$pilot_dir/pom.xml" | head -1)
EOF
