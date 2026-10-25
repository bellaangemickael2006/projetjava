#!/bin/bash
set -e
echo "=== Tests VitaTrack Java ==="

SRC=src
OUT=build/test-classes
RES=src/main/resources

mkdir -p $OUT

# Compiler sources + tests ensemble
find $SRC -name "*.java" > /tmp/all_sources.txt
javac -d $OUT @/tmp/all_sources.txt
cp -r $RES/* $OUT/

echo ""
echo "--- JsonTest ---"
java -cp $OUT com.vitatrack.JsonTest

echo ""
echo "--- SessionTest ---"
java -cp $OUT com.vitatrack.SessionTest

echo ""
echo "=== Tous les tests passés ==="
