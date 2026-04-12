package de.moritzf.picoboard.scratch

/**
 * Controls how a sprite visually rotates when its [ScratchSprite.direction] changes.
 *
 * Mirrors the three rotation styles available in Scratch.
 */
public enum class ScratchRotationStyle {
    /**
     * The sprite image rotates freely to match the current direction.
     */
    ALL_AROUND,

    /**
     * The sprite only flips horizontally depending on whether it faces left or right.
     * It does not rotate for any other direction.
     */
    LEFT_RIGHT,

    /**
     * The sprite image never rotates regardless of the current direction.
     */
    DONT_ROTATE,
}
