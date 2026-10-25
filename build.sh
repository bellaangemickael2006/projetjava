#!/bin/bash
set -e
echo "=== Compilation VitaTrack Java ==="

SRC=src/main/java
OUT=build/classes
RES=src/main/resources

mkdir -p $OUT

# Compiler tous les fichiers Java
find $SRC -name "*.java" > /tmp/sources.txt
javac -d $OUT @/tmp/sources.txt
echo "✓ Compilation OK"

# Copier les ressources
cp -r $RES/* $OUT/
echo "✓ Ressources copiées"

# Créer le JAR exécutable
jar cfe build/vitatrack.jar com.vitatrack.Main -C $OUT .
echo "✓ JAR créé : build/vitatrack.jar"
echo ""
echo "Pour lancer : java -jar build/vitatrack.jar"
echo "Variables d'environnement requises :"
echo "  SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_KEY"
