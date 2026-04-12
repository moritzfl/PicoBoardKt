package de.moritzf.picoboard.scratch.internal

import de.moritzf.picoboard.scratch.ScratchRotationStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScratchGeometryTest {
    @Test
    fun `movePoint follows Scratch direction semantics`() {
        val movedRight = movePoint(ScratchVector(0.0, 0.0), 10.0, 90.0)
        val movedUp = movePoint(ScratchVector(0.0, 0.0), 10.0, 0.0)

        assertEquals(10.0, movedRight.x, 0.0001)
        assertEquals(0.0, movedRight.y, 0.0001)
        assertEquals(0.0, movedUp.x, 0.0001)
        assertEquals(10.0, movedUp.y, 0.0001)
    }

    @Test
    fun `rectangle and circle collisions work with scaled shapes`() {
        val rectangle = ScratchRectangleShape(
            center = ScratchVector(0.0, 0.0),
            halfWidth = 50.0,
            halfHeight = 10.0,
            rotationDegrees = 0.0,
        )
        val touchingCircle = ScratchCircleShape(
            center = ScratchVector(45.0, 0.0),
            radius = 10.0,
        )
        val farCircle = ScratchCircleShape(
            center = ScratchVector(80.0, 0.0),
            radius = 10.0,
        )

        assertTrue(touching(touchingCircle, rectangle))
        assertFalse(touching(farCircle, rectangle))
    }

    @Test
    fun `rotated rectangles use oriented collision checks`() {
        val rotated = ScratchRectangleShape(
            center = ScratchVector(0.0, 0.0),
            halfWidth = 40.0,
            halfHeight = 10.0,
            rotationDegrees = 45.0,
        )
        val other = ScratchRectangleShape(
            center = ScratchVector(30.0, 0.0),
            halfWidth = 20.0,
            halfHeight = 10.0,
            rotationDegrees = 0.0,
        )

        assertTrue(touching(rotated, other))
    }

    @Test
    fun `bounceDirection reflects on vertical and horizontal edges`() {
        assertEquals(-45.0, bounceDirection(45.0, EdgeCollision(left = false, right = true, bottom = false, top = false)), 0.0001)
        assertEquals(135.0, bounceDirection(45.0, EdgeCollision(left = false, right = false, bottom = false, top = true)), 0.0001)
    }

    @Test
    fun `rotation mapping follows scratch styles`() {
        assertEquals(0.0, spriteRotationDegrees(90.0, ScratchRotationStyle.ALL_AROUND), 0.0001)
        assertEquals(90.0, spriteRotationDegrees(0.0, ScratchRotationStyle.ALL_AROUND), 0.0001)
        assertEquals(0.0, spriteRotationDegrees(10.0, ScratchRotationStyle.DONT_ROTATE), 0.0001)
    }
}
