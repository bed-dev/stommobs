package codes.bed.minestom.mobs.spawn

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.EntityCreature
import kotlin.math.atan2
import kotlin.math.sqrt

internal object SmoothLookController {
    fun smoothLookAt(entity: EntityCreature, target: Point) {
        val from = entity.position
        val dx = target.x() - from.x()
        // Use exact eye height so mobs look naturally at the target.
        val dy = target.y() - (from.y() + entity.eyeHeight)
        val dz = target.z() - from.z()
        val horizontal = sqrt((dx * dx) + (dz * dz))

        val desiredYaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val desiredPitch = if (horizontal < 0.001) {
            if (dy > 0.0) -89f else 89f
        } else if (horizontal < 2.0 && kotlin.math.abs(dy) < 1.0) {
            // Keep head level when the target is close and roughly at the same elevation.
            0f
        } else if (horizontal < 1.15) {
            // At close range, keep head mostly level to avoid sharp up/down snapping.
            Math.toDegrees(-atan2(dy, horizontal)).toFloat().coerceIn(-20f, 20f)
        } else {
            Math.toDegrees(-atan2(dy, horizontal)).toFloat().coerceIn(-70f, 70f)
        }

        val currentYaw = from.yaw()
        val currentPitch = from.pitch()
        val yawStep = 11f
        val pitchStep = 10f

        val nextYaw = currentYaw + clamp(wrapDegrees(desiredYaw - currentYaw), -yawStep, yawStep)
        val nextPitch = currentPitch + clamp(desiredPitch - currentPitch, -pitchStep, pitchStep)
        entity.setView(nextYaw, nextPitch)
    }

    private fun wrapDegrees(value: Float): Float {
        var wrapped = value % 360f
        if (wrapped >= 180f) wrapped -= 360f
        if (wrapped < -180f) wrapped += 360f
        return wrapped
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
}

