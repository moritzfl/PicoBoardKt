package de.moritzf.picoboard.scratch.examples.catchthefallingball

import de.moritzf.picoboard.scratch.ScratchRotationStyle
import de.moritzf.picoboard.scratch.scratchStage
import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import korlibs.image.color.Colors
import kotlinx.coroutines.runBlocking

private const val STAGE_WIDTH: Int = 1000
private const val STAGE_HEIGHT: Int = 700

fun main(args: Array<String>): Unit {
    relaunchScratchMainWithModuleAccessIfNeeded(
        mainClassName = "de.moritzf.picoboard.scratch.examples.catchthefallingball.MainKt",
        args = args,
    )

    runBlocking {
        scratchStage(
            width = STAGE_WIDTH,
            height = STAGE_HEIGHT,
            title = "Catch The Falling Ball",
            backgroundColor = Colors.DARKSLATEGRAY,
        ) {
            rectangle(
                width = 190.0,
                height = 26.0,
                color = Colors.GOLD,
            ) {
                goTo(0.0, -285.0)
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            circle(
                radius = 20.0,
                color = Colors.CORAL,
            ) {
                goTo(0.0, 285.0)
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            // Aufgabe 1:
            // Bewege den Fänger nach links und rechts.
            //
            // Aufgabe 2:
            // Lasse den Ball nach unten fallen.
            //
            // Aufgabe 3:
            // Wenn der Ball den Fänger berührt, soll der Ball wieder oben erscheinen.
            //
            // Aufgabe 4:
            // Zähle mit, wie viele Bälle gefangen wurden.
        }
    }
}
