package de.moritzf.picoboard.scratch.internal

import de.moritzf.picoboard.scratch.ScratchRotationStyle
import korlibs.image.bitmap.Bitmap32
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class ScratchVector(
    val x: Double,
    val y: Double,
) {
    operator fun plus(other: ScratchVector): ScratchVector = ScratchVector(x + other.x, y + other.y)

    operator fun minus(other: ScratchVector): ScratchVector = ScratchVector(x - other.x, y - other.y)

    operator fun times(scale: Double): ScratchVector = ScratchVector(x * scale, y * scale)

    fun dot(other: ScratchVector): Double = (x * other.x) + (y * other.y)

    fun lengthSquared(): Double = dot(this)
}

internal sealed interface ScratchShape {
    fun axisAlignedBounds(): ScratchBounds
}

internal data class ScratchCircleShape(
    val center: ScratchVector,
    val radius: Double,
) : ScratchShape {
    override fun axisAlignedBounds(): ScratchBounds {
        return ScratchBounds(
            minX = center.x - radius,
            maxX = center.x + radius,
            minY = center.y - radius,
            maxY = center.y + radius,
        )
    }
}

internal data class ScratchRectangleShape(
    val center: ScratchVector,
    val halfWidth: Double,
    val halfHeight: Double,
    val rotationDegrees: Double,
) : ScratchShape {
    val rotationRadians: Double get() = Math.toRadians(rotationDegrees)

    fun axisX(): ScratchVector = ScratchVector(cos(rotationRadians), sin(rotationRadians))

    fun axisY(): ScratchVector = ScratchVector(-sin(rotationRadians), cos(rotationRadians))

    fun corners(): List<ScratchVector> {
        val axisX = axisX()
        val axisY = axisY()
        return listOf(
            center + (axisX * halfWidth) + (axisY * halfHeight),
            center + (axisX * halfWidth) - (axisY * halfHeight),
            center - (axisX * halfWidth) + (axisY * halfHeight),
            center - (axisX * halfWidth) - (axisY * halfHeight),
        )
    }

    override fun axisAlignedBounds(): ScratchBounds {
        val cosTheta = abs(cos(rotationRadians))
        val sinTheta = abs(sin(rotationRadians))
        val extentX = (cosTheta * halfWidth) + (sinTheta * halfHeight)
        val extentY = (sinTheta * halfWidth) + (cosTheta * halfHeight)
        return ScratchBounds(
            minX = center.x - extentX,
            maxX = center.x + extentX,
            minY = center.y - extentY,
            maxY = center.y + extentY,
        )
    }
}

// Pixel sampling step in world-space units. Smaller = more accurate but more expensive.
private const val PIXEL_SAMPLE_STEP = 2.0

internal data class ScratchImageShape(
    val center: ScratchVector,
    val bitmap: Bitmap32,
    val imageWidth: Double,
    val imageHeight: Double,
    val scale: Double,
    // spriteRotationDegrees(direction, rotationStyle) — the CCW rotation in Scratch coords
    val rotationDegrees: Double,
    // true when LEFT_RIGHT style and the sprite faces left (image is mirrored horizontally)
    val facingLeft: Boolean,
) : ScratchShape {
    override fun axisAlignedBounds(): ScratchBounds {
        val halfW = (imageWidth * scale) / 2.0
        val halfH = (imageHeight * scale) / 2.0
        val rotRad = Math.toRadians(rotationDegrees)
        val cosR = abs(cos(rotRad))
        val sinR = abs(sin(rotRad))
        return ScratchBounds(
            minX = center.x - cosR * halfW - sinR * halfH,
            maxX = center.x + cosR * halfW + sinR * halfH,
            minY = center.y - sinR * halfW - cosR * halfH,
            maxY = center.y + sinR * halfW + cosR * halfH,
        )
    }

    // Returns true if the pixel at world position (worldX, worldY) is non-transparent.
    //
    // Transform chain (all in Scratch coordinates, y-up):
    //   1. Translate: (dx, dy) = world point relative to sprite center
    //   2. Undo rotation: inverse-rotate by rotationDegrees (CCW in Scratch coords)
    //   3. Undo scale
    //   4. Optionally flip x for LEFT_RIGHT mirror
    //   5. Convert to pixel coords: flip y (image y goes down, Scratch y goes up)
    //      pixelX = localX + imageWidth/2
    //      pixelY = -localY + imageHeight/2
    internal fun containsPoint(worldX: Double, worldY: Double): Boolean {
        val dx = worldX - center.x
        val dy = worldY - center.y

        // Inverse of CCW rotation by rotationDegrees
        val rotRad = Math.toRadians(rotationDegrees)
        val cosR = cos(rotRad)
        val sinR = sin(rotRad)
        val localX = (dx * cosR + dy * sinR) / scale
        val localY = (-dx * sinR + dy * cosR) / scale

        val finalX = if (facingLeft) -localX else localX

        val pixelX = (finalX + imageWidth / 2.0).toInt()
        val pixelY = (-localY + imageHeight / 2.0).toInt()

        if (pixelX < 0 || pixelX >= imageWidth.toInt() || pixelY < 0 || pixelY >= imageHeight.toInt()) return false

        return bitmap[pixelX, pixelY].a > 0
    }
}

internal data class ScratchBounds(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
) {
    fun touchesStage(stageHalfWidth: Double, stageHalfHeight: Double): EdgeCollision {
        return EdgeCollision(
            left = minX < -stageHalfWidth,
            right = maxX > stageHalfWidth,
            bottom = minY < -stageHalfHeight,
            top = maxY > stageHalfHeight,
        )
    }
}

internal data class EdgeCollision(
    val left: Boolean,
    val right: Boolean,
    val bottom: Boolean,
    val top: Boolean,
) {
    val hitVerticalEdge: Boolean get() = left || right

    val hitHorizontalEdge: Boolean get() = top || bottom

    val hasCollision: Boolean get() = hitVerticalEdge || hitHorizontalEdge
}

internal fun movePoint(
    origin: ScratchVector,
    steps: Double,
    directionDegrees: Double,
): ScratchVector {
    return origin + directionVector(directionDegrees) * steps
}

internal fun directionVector(directionDegrees: Double): ScratchVector {
    val radians = Math.toRadians(directionDegrees)
    return ScratchVector(
        x = sin(radians),
        y = cos(radians),
    )
}

internal fun vectorToDirection(vector: ScratchVector): Double {
    return normalizeDirection(Math.toDegrees(atan2(vector.x, vector.y)))
}

internal fun normalizeDirection(directionDegrees: Double): Double {
    var value = directionDegrees
    while (value <= -180.0) value += 360.0
    while (value > 180.0) value -= 360.0
    return value
}

internal fun touching(left: ScratchShape, right: ScratchShape): Boolean {
    return when {
        left is ScratchCircleShape && right is ScratchCircleShape -> circlesTouch(left, right)
        left is ScratchRectangleShape && right is ScratchRectangleShape -> rectanglesTouch(left, right)
        left is ScratchCircleShape && right is ScratchRectangleShape -> circleTouchesRectangle(left, right)
        left is ScratchRectangleShape && right is ScratchCircleShape -> circleTouchesRectangle(right, left)
        left is ScratchImageShape || right is ScratchImageShape -> pixelTouching(left, right)
        else -> false
    }
}

// Pixel-perfect collision: samples world-space points in the AABB overlap and checks whether
// each point lies inside both shapes simultaneously.
private fun pixelTouching(left: ScratchShape, right: ScratchShape): Boolean {
    val lb = left.axisAlignedBounds()
    val rb = right.axisAlignedBounds()

    val overlapMinX = max(lb.minX, rb.minX)
    val overlapMaxX = min(lb.maxX, rb.maxX)
    val overlapMinY = max(lb.minY, rb.minY)
    val overlapMaxY = min(lb.maxY, rb.maxY)

    if (overlapMinX >= overlapMaxX || overlapMinY >= overlapMaxY) return false

    var sampleX = overlapMinX
    while (sampleX <= overlapMaxX) {
        var sampleY = overlapMinY
        while (sampleY <= overlapMaxY) {
            if (inShape(left, sampleX, sampleY) && inShape(right, sampleX, sampleY)) return true
            sampleY += PIXEL_SAMPLE_STEP
        }
        sampleX += PIXEL_SAMPLE_STEP
    }
    return false
}

private fun inShape(shape: ScratchShape, x: Double, y: Double): Boolean {
    return when (shape) {
        is ScratchCircleShape -> {
            val dx = x - shape.center.x
            val dy = y - shape.center.y
            (dx * dx + dy * dy) <= (shape.radius * shape.radius)
        }
        is ScratchRectangleShape -> {
            val local = rotate(ScratchVector(x, y) - shape.center, -shape.rotationRadians)
            abs(local.x) <= shape.halfWidth && abs(local.y) <= shape.halfHeight
        }
        is ScratchImageShape -> shape.containsPoint(x, y)
    }
}

internal fun spriteRotationDegrees(directionDegrees: Double, rotationStyle: ScratchRotationStyle): Double {
    return when (rotationStyle) {
        ScratchRotationStyle.ALL_AROUND -> 90.0 - directionDegrees
        ScratchRotationStyle.LEFT_RIGHT,
        ScratchRotationStyle.DONT_ROTATE,
        -> 0.0
    }
}

internal fun displayRotationDegrees(directionDegrees: Double, rotationStyle: ScratchRotationStyle): Double {
    return when (rotationStyle) {
        ScratchRotationStyle.ALL_AROUND -> directionDegrees - 90.0
        ScratchRotationStyle.LEFT_RIGHT,
        ScratchRotationStyle.DONT_ROTATE,
        -> 0.0
    }
}

internal fun clampPositionInsideStage(
    center: ScratchVector,
    bounds: ScratchBounds,
    stageHalfWidth: Double,
    stageHalfHeight: Double,
): ScratchVector {
    val offsetLeft = center.x - bounds.minX
    val offsetRight = bounds.maxX - center.x
    val offsetBottom = center.y - bounds.minY
    val offsetTop = bounds.maxY - center.y

    return ScratchVector(
        x = center.x.coerceIn(-stageHalfWidth + offsetLeft, stageHalfWidth - offsetRight),
        y = center.y.coerceIn(-stageHalfHeight + offsetBottom, stageHalfHeight - offsetTop),
    )
}

internal fun bounceDirection(
    directionDegrees: Double,
    edgeCollision: EdgeCollision,
): Double {
    val original = directionVector(directionDegrees)
    val reflected = ScratchVector(
        x = if (edgeCollision.hitVerticalEdge) -original.x else original.x,
        y = if (edgeCollision.hitHorizontalEdge) -original.y else original.y,
    )
    return vectorToDirection(reflected)
}

private fun circlesTouch(left: ScratchCircleShape, right: ScratchCircleShape): Boolean {
    val delta = left.center - right.center
    val radiusSum = left.radius + right.radius
    return delta.lengthSquared() <= radiusSum * radiusSum
}

private fun rectanglesTouch(left: ScratchRectangleShape, right: ScratchRectangleShape): Boolean {
    val axes = listOf(left.axisX(), left.axisY(), right.axisX(), right.axisY())
    return axes.none { axis ->
        val leftProjection = project(left.corners(), axis)
        val rightProjection = project(right.corners(), axis)
        leftProjection.second < rightProjection.first || rightProjection.second < leftProjection.first
    }
}

private fun circleTouchesRectangle(circle: ScratchCircleShape, rectangle: ScratchRectangleShape): Boolean {
    val translated = circle.center - rectangle.center
    val local = rotate(translated, -rectangle.rotationRadians)
    val closest = ScratchVector(
        x = local.x.coerceIn(-rectangle.halfWidth, rectangle.halfWidth),
        y = local.y.coerceIn(-rectangle.halfHeight, rectangle.halfHeight),
    )
    val delta = local - closest
    return delta.lengthSquared() <= circle.radius * circle.radius
}

private fun rotate(vector: ScratchVector, radians: Double): ScratchVector {
    val cosTheta = cos(radians)
    val sinTheta = sin(radians)
    return ScratchVector(
        x = (vector.x * cosTheta) - (vector.y * sinTheta),
        y = (vector.x * sinTheta) + (vector.y * cosTheta),
    )
}

private fun project(points: List<ScratchVector>, axis: ScratchVector): Pair<Double, Double> {
    var minProjection = Double.POSITIVE_INFINITY
    var maxProjection = Double.NEGATIVE_INFINITY
    for (point in points) {
        val projection = point.dot(axis)
        minProjection = min(minProjection, projection)
        maxProjection = max(maxProjection, projection)
    }
    return minProjection to maxProjection
}

