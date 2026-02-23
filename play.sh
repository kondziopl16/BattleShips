#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# play.sh — uruchamia klienta turnieju Battleships
#
# Użycie:
#   ./play.sh [SERVER] [NAME]
#
# Przykłady:
#   ./play.sh                                     # localhost:8080, losowa nazwa
#   ./play.sh http://localhost:8080               # własny serwer, losowa nazwa
#   ./play.sh http://localhost:8080 MojBot         # własny serwer i nazwa
#
# Kilka instancji (osobne okna terminala):
#   ./play.sh http://localhost:8080 Bot-1
#   ./play.sh http://localhost:8080 Bot-2
#   ./play.sh http://localhost:8080 Bot-3
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SERVER="${1:-http://localhost:8080}"

if [ -n "${2:-}" ]; then
    NAME="$2"
else
    # Generuj unikalną nazwę na podstawie PID + czasu
    NAME="SmartAI-$$"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "┌──────────────────────────────────────┐"
echo "│   Battleships Tournament Client      │"
echo "├──────────────────────────────────────┤"
echo "│ Serwer : $SERVER"
echo "│ Nazwa  : $NAME"
echo "└──────────────────────────────────────┘"
echo ""

# Wybierz sposób uruchomienia: jar (szybciej po buildzie) lub gradlew
JAR="$SCRIPT_DIR/build/libs/battleships-1.0.0.jar"

if [ -f "$JAR" ]; then
    echo "[INFO] Uruchamiam z JAR: $JAR"
    java -jar "$JAR" --tournament --server "$SERVER" --name "$NAME"
else
    echo "[INFO] JAR nie znaleziony — buduję i uruchamiam przez Gradle..."
    if [ -f "$SCRIPT_DIR/gradlew" ]; then
        "$SCRIPT_DIR/gradlew" run --args="--tournament --server $SERVER --name $NAME" -q
    else
        gradle run --args="--tournament --server $SERVER --name $NAME" -q
    fi
fi
