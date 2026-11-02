#!/usr/bin/env sh
# Build and play. Extra arguments go to the game, e.g. ./play.sh --difficulty hard
set -e
cd "$(dirname "$0")"
command -v javac >/dev/null || { echo "Java 17 or newer (JDK) is required: https://adoptium.net"; exit 1; }
rm -rf out
javac -d out -sourcepath src/main/java src/main/java/hauntedcastle/Main.java
cp -r src/main/resources/. out/
java -cp out hauntedcastle.Main "$@"
