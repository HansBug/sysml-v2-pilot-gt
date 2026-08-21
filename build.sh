#!/usr/bin/env bash
set -euo pipefail

version=${1:-${REVISION:-0.1.0-SNAPSHOT}}
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

"$repo_root/scripts/build-official.sh" "$repo_root/target/vendor" "$version"
"$repo_root/mvnw" -q org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
  -Dfile="$repo_root/target/vendor/pilot-runtime.jar" \
  -DpomFile="$repo_root/scripts/official-runtime-pom.xml" \
  -DgroupId=org.omg.sysml -DartifactId=official-pilot-runtime \
  -Dversion=local -Dpackaging=jar
exec "$repo_root/mvnw" -B -Drevision="$version" verify
