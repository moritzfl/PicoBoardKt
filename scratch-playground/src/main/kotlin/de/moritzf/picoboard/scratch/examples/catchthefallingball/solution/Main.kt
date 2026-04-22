package de.moritzf.picoboard.scratch.examples.catchthefallingball.solution

import de.moritzf.picoboard.easy.PicoBoardEasy
import de.moritzf.picoboard.easy.PicoBoardService
import de.moritzf.picoboard.scratch.ScratchRectangleSprite
import de.moritzf.picoboard.scratch.ScratchRotationStyle
import de.moritzf.picoboard.scratch.ScratchStage
import de.moritzf.picoboard.scratch.internal.relaunchScratchMainWithModuleAccessIfNeeded
import de.moritzf.picoboard.scratch.scratchStage
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.text.TextAlignment
import kotlinx.coroutines.runBlocking
import kotlin.math.min
import kotlin.random.Random

private const val STAGE_WIDTH: Int = 1000
private const val STAGE_HEIGHT: Int = 700
private const val CATCHER_WIDTH: Double = 190.0
private const val CATCHER_HEIGHT: Double = 26.0
private const val CATCHER_Y: Double = -285.0
private const val CATCHER_SPEED: Double = 18.0
private const val BALL_RADIUS: Double = 20.0
private const val BALL_START_SPEED: Double = 7.0
private const val BALL_SPEED_STEP: Double = 0.6
private const val BALL_MAX_SPEED: Double = 16.0
private const val LIFE_ICON_RADIUS: Double = 10.0
private const val START_LIVES: Int = 3

// The game loop runs at ~60 fps (~16 ms/frame). Poll at least 2x that rate so every
// frame sees a reasonably fresh reading.
private const val GAME_LOOP_INTERVAL_MILLIS: Long = 16L
private const val PICOBOARD_POLL_INTERVAL_MILLIS: Long = GAME_LOOP_INTERVAL_MILLIS / 2

private enum class GameState {
    READY,
    PLAYING,
    GAME_OVER,
}

@Suppress("LongMethod", "MagicNumber")
fun main(args: Array<String>): Unit {
    relaunchScratchMainWithModuleAccessIfNeeded(
        mainClassName = "de.moritzf.picoboard.scratch.examples.catchthefallingball.solution.MainKt",
        args = args,
    )

    runBlocking {
        println("Starting Catch The Falling Ball.")
        println("Keyboard fallback: Left/Right to move, Space to start or restart.")

        var picoService = runCatching {
            PicoBoardEasy.startService(intervalMillis = PICOBOARD_POLL_INTERVAL_MILLIS)
        }.onSuccess {
            println(
                "Connected to PicoBoard on '${it.portIdentifier}'. " +
                    "Use the slider to move and the button to start or restart.",
            )
        }.onFailure { failure ->
            println(
                "No PicoBoard was auto-selected (${failure.message ?: failure::class.simpleName}). " +
                    "Using keyboard controls.",
            )
        }.getOrNull()

        scratchStage(
            width = STAGE_WIDTH,
            height = STAGE_HEIGHT,
            title = "PicoBoard Catch The Falling Ball",
            backgroundColor = Colors.DARKSLATEGRAY,
        ) {
            onClose {
                picoService?.close()
            }

            val random = Random(System.nanoTime())

            val catcher = rectangle(
                width = CATCHER_WIDTH,
                height = CATCHER_HEIGHT,
                color = Colors.GOLD,
            ) {
                goTo(0.0, CATCHER_Y)
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            val ball = circle(
                radius = BALL_RADIUS,
                color = Colors.CORAL,
            ) {
                rotationStyle = ScratchRotationStyle.DONT_ROTATE
            }

            val lifeIcons = List(START_LIVES) { index ->
                circle(
                    radius = LIFE_ICON_RADIUS,
                    color = Colors.CRIMSON,
                ) {
                    goTo(
                        x = -(width / 2.0) + 28.0 + (index * 28.0),
                        y = (height / 2.0) - 28.0,
                    )
                    rotationStyle = ScratchRotationStyle.DONT_ROTATE
                }
            }

            val gameOverText = text(fontSize = 40.0) {
                hide()
            }

            val scoreText = text(
                fontSize = 28.0,
                alignment = TextAlignment.TOP_RIGHT,
            ) {
                goTo(x = width / 2.0 - 16.0, y = height / 2.0 - 16.0)
                hide()
            }

            var score = 0
            var lives = START_LIVES
            var ballSpeed = BALL_START_SPEED
            var gameState = GameState.READY
            var actionButtonWasPressed = false

            fun updateLifeIcons(): Unit {
                lifeIcons.forEachIndexed { index, icon ->
                    icon.visible = index < lives
                }
            }

            fun resetBall(): Unit {
                val margin = ball.radius + 24.0
                val spawnX = random.nextDouble(
                    from = -(width / 2.0) + margin,
                    until = (width / 2.0) - margin,
                )
                val spawnY = (height / 2.0) - ball.radius - 32.0

                ball.goTo(spawnX, spawnY)
                ball.pointInDirection(180.0)
            }

            fun prepareNewGame(printInstructions: Boolean): Unit {
                score = 0
                lives = START_LIVES
                ballSpeed = BALL_START_SPEED
                gameState = GameState.READY
                catcher.goTo(0.0, CATCHER_Y)
                gameOverText.hide()
                scoreText.hide()
                updateLifeIcons()
                resetBall()

                if (printInstructions) {
                    println("Press Space or the PicoBoard button to start catching balls.")
                }
            }

            fun startGame(): Unit {
                gameState = GameState.PLAYING
                scoreText.text = "Score: 0"
                scoreText.show()
                println("Game started.")
            }

            fun restartGame(): Unit {
                prepareNewGame(printInstructions = false)
                startGame()
            }

            prepareNewGame(printInstructions = true)

            forever {
                if (picoService != null && !picoService!!.isRunning()) {
                    val failure = picoService!!.failure()
                    println(
                        "PicoBoard polling stopped (${failure?.message ?: "unknown reason"}). " +
                            "Using keyboard controls.",
                    )
                    picoService!!.close()
                    picoService = null
                }

                val service = picoService
                updateCatcherPosition(catcher, service)

                val actionButtonPressed = (service?.buttonPressed() == true) || keyPressed(Key.SPACE)
                if (actionButtonPressed && !actionButtonWasPressed) {
                    when (gameState) {
                        GameState.READY -> startGame()
                        GameState.GAME_OVER -> restartGame()
                        GameState.PLAYING -> Unit
                    }
                }
                actionButtonWasPressed = actionButtonPressed

                if (gameState != GameState.PLAYING) {
                    return@forever
                }

                ball.move(ballSpeed)

                if (ball.touching(catcher)) {
                    score += 1
                    scoreText.text = "Score: $score"
                    ballSpeed = min(BALL_MAX_SPEED, ballSpeed + BALL_SPEED_STEP)
                    println("Caught it. Score: $score")
                    resetBall()
                    return@forever
                }

                if (ball.y < -(height / 2.0) - (ball.radius * ball.scale)) {
                    lives -= 1
                    updateLifeIcons()

                    if (lives <= 0) {
                        gameState = GameState.GAME_OVER
                        resetBall()
                        scoreText.hide()
                        gameOverText.text = "Game Over - Score: $score"
                        gameOverText.show()
                        println("Game over. Final score: $score. Press Space or the PicoBoard button to try again.")
                    } else {
                        println("Missed it. Lives left: $lives")
                        resetBall()
                    }
                }
            }
        }
    }
}

private fun ScratchStage.updateCatcherPosition(
    catcher: ScratchRectangleSprite,
    service: PicoBoardService?,
): Unit {
    val maxCatcherX = (width / 2.0) - ((catcher.width * catcher.scale) / 2.0) - 8.0

    if (service != null) {
        val normalizedSlider = (service.slider() / 100.0) * 2.0 - 1.0
        catcher.x = (normalizedSlider * maxCatcherX).coerceIn(-maxCatcherX, maxCatcherX)
        return
    }

    var delta = 0.0
    if (keyPressed(Key.LEFT)) {
        delta -= CATCHER_SPEED
    }
    if (keyPressed(Key.RIGHT)) {
        delta += CATCHER_SPEED
    }

    if (delta != 0.0) {
        catcher.x = (catcher.x + delta).coerceIn(-maxCatcherX, maxCatcherX)
    }
}
