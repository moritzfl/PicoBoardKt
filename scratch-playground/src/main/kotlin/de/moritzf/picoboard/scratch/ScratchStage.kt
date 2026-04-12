package de.moritzf.picoboard.scratch

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.view.Stage
import korlibs.korge.view.addUpdater
import korlibs.korge.view.circle
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import korlibs.render.GameWindowCreationConfig
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Opens a resizable game window and runs [init] to set up the stage.
 *
 * The coordinate system follows Scratch conventions: the origin (0, 0) is the center of the
 * stage, x increases to the right, and y increases upward.
 *
 * @param width logical width of the stage in pixels.
 * @param height logical height of the stage in pixels.
 * @param title window title shown in the title bar.
 * @param backgroundColor color rendered behind all sprites.
 * @param maxInitialWindowDimension the largest the initial window will be on either axis.
 *   The window is scaled down proportionally if the stage is larger than this value.
 * @param init block that sets up sprites, registers the game loop, and configures callbacks.
 */
@Suppress("MagicNumber")
public suspend fun scratchStage(
    width: Int,
    height: Int,
    title: String = "Scratch Stage",
    backgroundColor: RGBA = Colors["#F5F1E8"],
    maxInitialWindowDimension: Int = 900,
    init: ScratchStage.() -> Unit,
): Unit {
    require(width > 0) {
        "width must be greater than zero"
    }
    require(height > 0) {
        "height must be greater than zero"
    }
    require(maxInitialWindowDimension > 0) {
        "maxInitialWindowDimension must be greater than zero"
    }

    val initialWindow = fitIntoBoundingBox(width, height, maxInitialWindowDimension)
    Korge(
        windowSize = Size(initialWindow.first, initialWindow.second),
        virtualSize = Size(width, height),
        displayMode = KorgeDisplayMode.CENTER,
        title = title,
        backgroundColor = backgroundColor,
        windowCreationConfig = GameWindowCreationConfig(resizable = true),
    ).start {
        ScratchStage(this, width.toDouble(), height.toDouble()).init()
    }
}

/**
 * A Scratch-style stage that hosts sprites and drives the game loop.
 *
 * Positions are expressed in Scratch coordinates: the origin (0, 0) is the center of the
 * stage, x increases to the right, and y increases upward. [width] and [height] are the full
 * logical dimensions, so the reachable x range is `[-width/2, width/2]` and the reachable y
 * range is `[-height/2, height/2]`.
 */
public class ScratchStage internal constructor(
    private val korgeStage: Stage,
    /** Logical width of the stage in pixels. */
    public val width: Double,
    /** Logical height of the stage in pixels. */
    public val height: Double,
) {
    internal val stageHalfWidth: Double = width / 2.0
    internal val stageHalfHeight: Double = height / 2.0
    internal val stageCenterX: Double = width / 2.0
    internal val stageCenterY: Double = height / 2.0

    /**
     * Creates a filled rectangle sprite and adds it to the stage.
     *
     * The sprite is initially positioned at the stage center (0, 0). Use the [init] block or
     * the sprite's [ScratchSprite.goTo] method to move it after creation.
     *
     * @param width width of the rectangle in pixels.
     * @param height height of the rectangle in pixels.
     * @param color fill color.
     * @param init optional configuration block run immediately after the sprite is created.
     * @return the created [ScratchRectangleSprite].
     */
    @JvmOverloads
    public fun rectangle(
        width: Double,
        height: Double,
        color: RGBA = Colors.WHITE,
        init: ScratchRectangleSprite.() -> Unit = {},
    ): ScratchRectangleSprite {
        val sprite = ScratchRectangleSprite(
            stage = this,
            view = korgeStage.solidRect(width, height, color),
            width = width,
            height = height,
        )
        return sprite.apply(init)
    }

    /**
     * Creates a filled circle sprite and adds it to the stage.
     *
     * The sprite is initially positioned at the stage center (0, 0). Use the [init] block or
     * the sprite's [ScratchSprite.goTo] method to move it after creation.
     *
     * @param radius radius of the circle in pixels.
     * @param color fill color.
     * @param init optional configuration block run immediately after the sprite is created.
     * @return the created [ScratchCircleSprite].
     */
    @JvmOverloads
    public fun circle(
        radius: Double,
        color: RGBA = Colors.WHITE,
        init: ScratchCircleSprite.() -> Unit = {},
    ): ScratchCircleSprite {
        val sprite = ScratchCircleSprite(
            stage = this,
            view = korgeStage.circle(radius, fill = color, stroke = color),
            radius = radius,
        )
        return sprite.apply(init)
    }

    /**
     * Creates a text label and adds it to the stage.
     *
     * The label is initially positioned at the stage center (0, 0) and hidden. Use the [init]
     * block to move it and set its initial text before showing it.
     *
     * The [alignment] controls which point of the text bounding box the position refers to.
     * The default [TextAlignment.MIDDLE_CENTER] means x and y point to the center of the text,
     * consistent with how [circle] and [rectangle] sprites are positioned.
     *
     * @param text initial text content.
     * @param fontSize font size in pixels.
     * @param color text color.
     * @param alignment how the position relates to the text bounds.
     * @param init optional configuration block run immediately after the label is created.
     * @return the created [ScratchTextSprite].
     */
    @JvmOverloads
    public fun text(
        text: String = "",
        fontSize: Double = 32.0,
        color: RGBA = Colors.WHITE,
        alignment: TextAlignment = TextAlignment.MIDDLE_CENTER,
        init: ScratchTextSprite.() -> Unit = {},
    ): ScratchTextSprite {
        val view = korgeStage.text(text, textSize = fontSize, color = color, alignment = alignment)
        val sprite = ScratchTextSprite(this, view)
        return sprite.apply(init)
    }

    /**
     * Registers a block that is called once per frame for the lifetime of the stage.
     *
     * This is the main game loop entry point. Read sensor values, update sprite positions, and
     * check game conditions inside [block]. The block runs on the render thread; avoid blocking
     * calls.
     *
     * @param block code to execute every frame, with this stage as receiver.
     */
    public fun forever(block: ScratchStage.() -> Unit): Unit {
        korgeStage.addUpdater {
            block()
        }
    }

    /**
     * Returns `true` if [key] is currently held down.
     *
     * @param key the key to check.
     */
    public fun keyPressed(key: Key): Boolean {
        return korgeStage.views.input.keys[key]
    }

    /**
     * Registers a suspend [block] that is called when the game window is closed.
     *
     * Use this to release resources such as hardware connections.
     *
     * @param block suspend function to call on window close.
     */
    public fun onClose(block: suspend () -> Unit): Unit {
        korgeStage.views.onClose(block)
    }
}

private fun fitIntoBoundingBox(
    width: Int,
    height: Int,
    maxDimension: Int,
): Pair<Int, Int> {
    val scale = minOf(maxDimension / width.toDouble(), maxDimension / height.toDouble(), 1.0)
    return max(320, (width * scale).roundToInt()) to max(240, (height * scale).roundToInt())
}
