package de.moritzf.picoboard.examples.firstproject

import de.moritzf.picoboard.easy.PicoBoardEasy

fun main() {
    println("Move the slider, cover the light sensor, or press the button.")

    PicoBoardEasy.run {
        every(100) {
            val message = when {
                buttonPressed() -> "The button is pressed"
                light() < 30 -> "It is dark"
                slider() > 50 -> "The slider is high"
                else -> "Try moving a sensor"
            }

            println(
                "slider=${slider()} " +
                "light=${light()} " +
                "sound=${sound()} " +
                "button=${buttonPressed()} -> $message",
            )
        }
    }
}

