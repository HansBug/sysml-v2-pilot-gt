#!/usr/bin/env bash
set -euo pipefail

version=${1:-${REVISION:-0.1.0-SNAPSHOT}}
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

if [[ ! "$version" =~ ^[0-9A-Za-z][0-9A-Za-z.-]*$ ]]; then
  echo "invalid Maven version: $version" >&2
  exit 2
fi

"$repo_root/scripts/build-official.sh" "$repo_root/target/vendor" "$version"
runtime_pom="$repo_root/target/vendor/official-runtime-pom.xml"
sed "s#<version>\${revision}</version>#<version>${version}</version>#" \
  "$repo_root/scripts/official-runtime-pom.xml" > "$runtime_pom"
"$repo_root/mvnw" -q org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
  -Dfile="$repo_root/target/vendor/pilot-runtime.jar" \
  -DpomFile="$runtime_pom" \
  -DgroupId=org.omg.sysml -DartifactId=official-pilot-runtime \
  -Dversion="$version" -Dpackaging=jar
exec "$repo_root/mvnw" -B -Drevision="$version" verify
