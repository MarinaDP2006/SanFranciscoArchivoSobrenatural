#!/usr/bin/env bash
# Genera la aplicación nativa (Linux/macOS) con jpackage. En Windows usa empaquetar-exe.bat
set -euo pipefail
cd "$(dirname "$0")"
mvn -q clean package -DskipTests
rm -rf dist build-exe && mkdir build-exe
cp desktop-admin/target/sfa-gestion.jar build-exe/
jpackage --type app-image --name "SF-Archive" --app-version 1.0.0 \
  --input build-exe --main-jar sfa-gestion.jar --main-class com.sfarchive.desktop.Launcher --dest dist
rm -rf build-exe
echo "Aplicación generada en dist/"
