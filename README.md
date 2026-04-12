# picoboard

`picoboard` is a Kotlin/JVM library for reading a Scratch-era PicoBoard from Kotlin and Java applications over a serial connection.

The library:

- uses a Gradle build with a standard wrapper
- works on macOS, Linux, and Windows through `jSerialComm`
- exposes both raw 10-bit sensor values and Scratch-style scaled values
- supports one-shot reads and scheduled polling

## Status

The implementation targets the classic PicoBoard serial protocol used by Scratch 1.x:

- `38400` baud, `8N1`
- host sends poll byte `0x01`
- board replies with an 18-byte packet made of nine high/low channel pairs

The parser and scaling logic follow the MIT Scratch Board technical guide, and the serial setup matches the SparkFun PicoBoard getting-started documentation.

## Build

```bash
./gradlew build
```

## Beginner Guides

- English: [BEGINNER_README.en.md](BEGINNER_README.en.md)
- Deutsch: [BEGINNER_README.de.md](BEGINNER_README.de.md)

The runnable Kotlin starter example is in:

[Main.kt](examples/first-project-kotlin/src/main/kotlin/de/moritzf/picoboard/examples/firstproject/Main.kt)

Run it with:

```bash
./gradlew runFirstProjectKotlin
```

## CLI Sample

List available serial ports:

```bash
./gradlew run --args="--list-ports"
```

Read continuously with library auto-selection:

```bash
./gradlew run --args="--interval-ms 100"
```

Read continuously from a specific PicoBoard:

```bash
./gradlew run --args="--port /dev/cu.usbserial-A5061E1Q --interval-ms 100"
```

Read 10 frames and exit:

```bash
./gradlew run --args="--port /dev/cu.usbserial-A5061E1Q --count 10"
```

Install the CLI with startup scripts:

```bash
./gradlew installDist
build/install/picoboard/bin/picoboard --port /dev/cu.usbserial-A5061E1Q --count 10
```

## Auto-Selection

The library can auto-select a suitable serial device:

- `PicoBoard.findAutoSelectedPort()` returns the uniquely best match or `null`
- `PicoBoard.requireAutoSelectedPort()` throws if no suitable device is found or if selection is ambiguous
- `PicoBoard.open()` auto-selects a suitable device and opens it

If you want full control, keep using `PicoBoard.open(portPath)` or `PicoBoard.open(port)`.

## Kotlin Example

```kotlin
import de.moritzf.picoboard.PicoBoard

fun main() {
    PicoBoard.open().use { board ->
        val frame = board.readFrame()

        println("Light: ${frame.scaled.light}")
        println("Sound: ${frame.scaled.sound}")
        println("Button pressed: ${frame.scaled.buttonPressed}")
        println("Resistance A raw: ${frame.raw.resistanceA}")
    }
}
```

## Java Example

```java
import de.moritzf.picoboard.PicoBoard;
import de.moritzf.picoboard.PicoBoardFrame;

public final class ReadPicoBoard {
    public static void main(String[] args) throws Exception {
        try (var board = PicoBoard.open()) {
            PicoBoardFrame frame = board.readFrame();
            System.out.println("Slider: " + frame.getScaled().getSlider());
            System.out.println("Button pressed: " + frame.getScaled().isButtonPressed());
        }
    }
}
```

## Notes

- On older systems, you may need FTDI VCP drivers installed before the PicoBoard appears as a serial device.
- On macOS, the relevant port is typically `/dev/tty.usbserial-*` or `/dev/cu.usbserial-*`.
- On Linux, it is commonly `/dev/ttyUSB*`.
- On Windows, it appears as `COMx`.
