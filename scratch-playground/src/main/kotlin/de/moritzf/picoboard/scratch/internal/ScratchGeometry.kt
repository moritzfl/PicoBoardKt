package de.moritzf.picoboard.scratch.internal

import de.moritzf.picoboard.scratch.ScratchRotationStyle
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
        else -> false
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

