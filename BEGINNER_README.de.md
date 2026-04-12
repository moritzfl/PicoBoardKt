# Einsteiger-Anleitung

Diese Anleitung ist für Schülerinnen und Schüler gedacht, die den PicoBoard zum ersten Mal benutzen.

Du musst am Anfang keine seriellen Schnittstellen, Polling-Schleifen oder die komplette Bibliothek verstehen.

## So Startest Du

1. Verbinde den PicoBoard mit dem Computer.
2. Öffne ein Terminal in diesem Projekt.
3. Starte das erste Beispiel:

```bash
./gradlew runFirstProjectKotlin
```

Wenn der PicoBoard gefunden wird, erscheinen im Terminal laufend Werte.

## Deine Erste Datei

Das Starter-Beispiel liegt hier:

[Main.kt](examples/first-project-kotlin/src/main/kotlin/de/moritzf/picoboard/examples/firstproject/Main.kt)

Öffne die Datei und ändere eine kleine Sache. Zum Beispiel:

- ändere den Text auf der Konsole
- ändere `slider() > 50` zu `slider() > 80`
- füge `println("Licht: ${light()}")` hinzu

## Einfache API

Die einfache API liegt im Paket `de.moritzf.picoboard.easy`.

Der wichtigste Einstiegspunkt ist:

```kotlin
import de.moritzf.picoboard.easy.PicoBoardEasy

fun main() {
    PicoBoardEasy.run {
        every(100) {
            println("Slider: ${slider()}")
        }
    }
}
```

Im `run { ... }`-Block wird der PicoBoard automatisch verbunden.

In `every(100) { ... }` gilt:

- das Board wird alle 100 Millisekunden gelesen
- die Sensorwerte stehen als einfache Funktionen zur Verfügung
- Lesefehler werden beim kontinuierlichen Polling automatisch erneut versucht

## Wichtige Funktionen

- `slider()` liefert einen Wert von `0` bis `100`
- `light()` liefert einen Wert von `0` bis `100`
- `sound()` liefert einen Wert von `0` bis `100`
- `buttonPressed()` liefert `true` oder `false`
- `resistanceA()`, `resistanceB()`, `resistanceC()`, `resistanceD()` liefern Werte von `0` bis `100`

Wenn du nur eine feste Anzahl von Messungen willst, kannst du stattdessen das hier verwenden:

```kotlin
PicoBoardEasy.run {
    repeat(times = 10, intervalMillis = 100) {
        println("Knopf: ${buttonPressed()}")
    }
}
```

## Wenn Es Nicht Funktioniert

Prüfe diese Punkte:

1. Ist der PicoBoard wirklich verbunden?
2. Verwendet kein anderes Programm gerade das Board?
3. Liste die seriellen Ports auf:

```bash
./gradlew run --args="--list-ports"
```

4. Auf manchen Computern werden FTDI-Treiber benötigt, bevor der PicoBoard als serielles Gerät sichtbar ist.

## Nächster Schritt

Wenn das Starter-Beispiel funktioniert, probiere ein eigenes kleines Projekt:

- gib eine Nachricht aus, wenn der Knopf gedrückt wird
- zeige an, ob es hell oder dunkel ist
- reagiere darauf, wenn der Slider über einen Grenzwert geht

Wenn du später mehr Kontrolle brauchst, kannst du von `de.moritzf.picoboard.easy` auf die Core-API in `de.moritzf.picoboard` wechseln.
