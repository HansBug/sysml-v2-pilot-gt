#!/usr/bin/env bash
set -euo pipefail

version=${1:?release version is required}
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
repository_id=${MAVEN_REPOSITORY_ID:-github}
repository_url=${MAVEN_REPOSITORY_URL:-https://maven.pkg.github.com/HansBug/sysml-v2-pilot-gt}
runtime_pom="$repo_root/target/vendor/official-runtime-pom.xml"

if [[ ! "$version" =~ ^[0-9A-Za-z][0-9A-Za-z.-]*$ ]]; then
  echo "invalid Maven version: $version" >&2
  exit 2
fi
if [[ ! -f "$repo_root/target/vendor/pilot-runtime.jar" || ! -f "$runtime_pom" ]]; then
  echo "built runtime is missing; run ./build.sh $version first" >&2
  exit 3
fi

"$repo_root/mvnw" -B org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file \
  -Dfile="$repo_root/target/vendor/pilot-runtime.jar" \
  -DpomFile="$runtime_pom" \
  -DgroupId=org.omg.sysml \
  -DartifactId=official-pilot-runtime \
  -Dversion="$version" \
  -Dpackaging=jar \
  -DrepositoryId="$repository_id" \
  -Durl="$repository_url"

"$repo_root/mvnw" -B -Drevision="$version" \
  -DaltDeploymentRepository="$repository_id::default::$repository_url" deploy
