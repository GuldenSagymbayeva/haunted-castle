#!/usr/bin/env sh
# Build and start the game. Extra arguments go to the game, e.g. ./run.sh --seed 42
set -e
cd "$(dirname "$0")"
rm -rf out
javac -d out $(find src -name '*.java')
java -cp out Main "$@"
