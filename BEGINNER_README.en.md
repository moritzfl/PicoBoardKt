# Beginner Guide

This guide is for students who are using the PicoBoard for the first time.

You do not need to understand serial ports, polling loops, or the full library API to start.

## Start Here

1. Connect the PicoBoard to your computer.
2. Open a terminal in this project.
3. Run the starter example:

```bash
./gradlew runFirstProjectKotlin
```

If the board is found, you should see changing values in the console.

The Gradle tasks in this repository use Java 21 toolchains.

## Your First File

The starter example is in:

[Main.kt](examples/first-project-kotlin/src/main/kotlin/de/moritzf/picoboard/examples/firstproject/Main.kt)

Open that file and change one thing. For example:

- change the text that is printed
- change `slider() > 50` to `slider() > 80`
- add `println("Light: ${light()}")`

## Beginner API

The beginner API is in `de.moritzf.picoboard.easy`.

The most important entry point is:

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

Inside the `run { ... }` block, the board is connected automatically.

Inside `every(100) { ... }`:

- the board is read every 100 milliseconds
- the current sensor values are available as simple functions
- read errors are retried automatically during continuous polling

## Useful Functions

- `slider()` returns a value from `0` to `100`
- `light()` returns a value from `0` to `100`
- `sound()` returns a value from `0` to `100`
- `buttonPressed()` returns `true` or `false`
- `resistanceA()`, `resistanceB()`, `resistanceC()`, `resistanceD()` return values from `0` to `100`

If you want a fixed number of reads instead of an endless loop, use:

```kotlin
PicoBoardEasy.run {
    repeat(times = 10, intervalMillis = 100) {
        println("Button: ${buttonPressed()}")
    }
}
```

## If It Does Not Work

Try these checks:

1. Make sure the PicoBoard is connected.
2. Make sure no other program is using the board.
3. Run the port listing command:

```bash
./gradlew run --args="--list-ports"
```

4. On some computers, FTDI drivers may be required before the PicoBoard appears as a serial device.

## Next Step

After the starter example works, try building a tiny project:

- print a message when the button is pressed
- show whether the room is bright or dark
- react when the slider goes above a value

When you need more control, you can move from `de.moritzf.picoboard.easy` to the core API in `de.moritzf.picoboard`.
