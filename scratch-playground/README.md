# scratch-playground

`scratch-playground` is an optional KorGE-based module that adds a Scratch-shaped 2D API on top of the PicoBoard library.

It is meant as the next step after the beginner PicoBoard examples:

- students still read PicoBoard values with the easy API
- they get a stage and simple ready-to-use objects similar to Scratch sprites
- they can build small games without dealing with low-level rendering setup first

## What It Provides

- `scratchStage(width, height, ...)` for a logical stage with a resizable window
- centered Scratch-like coordinates:
  `x = 0`, `y = 0` is the middle of the stage
- simple shapes:
  `rectangle(...)` and `circle(...)`
- sprite-style properties:
  `x`, `y`, `direction`, `size`, `scale`, `rotationStyle`, `visible`
- sprite-style helpers:
  `goTo(...)`, `move(...)`, `turnLeft(...)`, `turnRight(...)`, `touching(...)`, `touchingEdge()`, `ifOnEdgeBounce()`
- frame loops:
  `forever { ... }`

## Small Example

```kotlin
import de.moritzf.picoboard.scratch.ScratchRotationStyle
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.event.Key
import korlibs.image.color.Colors

suspend fun main() = scratchStage(width = 1000, height = 700, title = "My First Stage") {
    val player = rectangle(
        name = "Player",
        width = 140.0,
        height = 24.0,
        color = Colors["#E2C044"],
    ) {
        goTo(0.0, -250.0)
        rotationStyle = ScratchRotationStyle.DONT_ROTATE
    }

    val ball = circle(
        name = "Ball",
        radius = 16.0,
        color = Colors["#FF7F50"],
    ) {
        goTo(0.0, 40.0)
        pointInDirection(35.0)
    }

    forever {
        if (keyPressed(Key.LEFT)) {
            player.changeXBy(-6.0)
        }
        if (keyPressed(Key.RIGHT)) {
            player.changeXBy(6.0)
        }

        ball.move(6.0)
        ball.ifOnEdgeBounce()

        if (ball.touching(player)) {
            println("Hit")
        }
    }
}
```

## Catch The Falling Ball Example

The included example is here:

[Main.kt](src/main/kotlin/de/moritzf/picoboard/scratch/examples/catchthefallingball/Main.kt)

Run it from the repository root with:

```bash
./gradlew runCatchTheFallingBall
```

You can also run the module task directly:

```bash
./gradlew :scratch-playground:run
```

Controls:

- with PicoBoard:
  slider moves the catcher, button starts or restarts
- without PicoBoard:
  Left/Right arrow keys move the catcher, Space starts or restarts

The example first tries PicoBoard auto-selection. If no suitable device is found, it keeps running with keyboard controls.
