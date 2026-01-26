# Battleships AI

Implementacja gry w statki z zaawansowanym algorytmem AI opartym na mapie prawdopodobieństw (Probability Density Function).

## Wymagania

- Java 17+
- Gradle 8.5+

## Instalacja Gradle Wrapper

Przed pierwszym uruchomieniem wygeneruj Gradle wrapper:

```bash
gradle wrapper --gradle-version=8.5
```

## Uruchomienie

### Pojedyncza gra (z logowaniem)

Linux/macOS:
```bash
./gradlew run
```

Windows:
```cmd
gradlew.bat run
```

Lub bezpośrednio z Gradle:
```bash
gradle run
```

### Benchmark (SmartAI vs RandomAI, 1000 gier)

```bash
./gradlew run --args="--benchmark"
```

### Statystyki (SmartAI vs SmartAI, 100 gier)

```bash
./gradlew run --args="--stats"
```

## Budowanie JAR

```bash
./gradlew jar
java -jar build/libs/battleships-1.0.0.jar
```

## Struktura projektu

```
battleships/
├── src/main/kotlin/
│   ├── core/           # Typy bazowe, Ship, Board, GameRules
│   ├── engine/         # GameEngine, GameRunner
│   ├── ai/             # Player interface, SmartAIPlayer
│   │   └── strategies/ # ProbabilityCalculator, HuntMode, TargetMode
│   ├── logging/        # GameLogger
│   └── Main.kt
└── build.gradle.kts
```

## Algorytm AI

### Strategia strzelania (Probability Density Function)

1. **Hunt Mode** - szukanie statków:
   - Oblicza mapę prawdopodobieństw dla każdej komórki
   - Uwzględnia pozostałe statki, pudła, zatopienia
   - Stosuje optymalizację parzystości (checkerboard pattern)

2. **Target Mode** - zatapanie trafionych statków:
   - Analizuje łańcuchy trafień
   - Rozszerza w kierunku zgodnym z orientacją statku
   - Obsługuje przypadki wielu statków

### Strategia rozmieszczania

- Generuje 1000 losowych rozmieszczein
- Wybiera najlepsze wg scoringu:
  - Odległość od krawędzi
  - Rozproszenie statków

## Format logów

```
HH:mm:ss.SSS place-ship: size=4 pos=(0,0) dir=horizontal
HH:mm:ss.SSS shot: pos=(4,2) result=miss
HH:mm:ss.SSS enemy-shot: pos=(3,1) result=hit
HH:mm:ss.SSS shot: pos=(5,3) result=sunk ship-size=2
HH:mm:ss.SSS game-over: result=win total-shots=47 enemy-total-shots=52
HH:mm:ss.SSS enemy-ship: size=4 pos=(2,3) dir=vertical
```

## Oczekiwane wyniki

| Przeciwnik | Win Rate |
|------------|----------|
| Random AI | >95% |
| Basic Probability AI | 65-70% |
| Inny SmartAI | ~50% |
# BattleShips
