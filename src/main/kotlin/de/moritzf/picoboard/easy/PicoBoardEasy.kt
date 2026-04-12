package de.moritzf.picoboard.easy

import de.moritzf.picoboard.PicoBoard
import de.moritzf.picoboard.PicoBoardConnection
import de.moritzf.picoboard.PicoBoardException
import de.moritzf.picoboard.PicoBoardFrame
import de.moritzf.picoboard.PicoBoardOptions
import de.moritzf.picoboard.PicoBoardScaledValues

public object PicoBoardEasy {
    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun run(
        portPath: String? = null,
        options: PicoBoardOptions = PicoBoardOptions(),
        block: PicoBoardProject.() -> Unit,
    ): Unit {
        openConnection(portPath, options).use { connection ->
            PicoBoardProject(connection).block()
        }
    }

    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun connect(
        portPath: String? = null,
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardProject {
        return PicoBoardProject(openConnection(portPath, options))
    }

    private fun openConnection(
        portPath: String?,
        options: PicoBoardOptions,
    ): PicoBoardConnection {
        return if (portPath == null) {
            PicoBoard.open(options)
        } else {
            PicoBoard.open(portPath, options)
        }
    }
}

public class PicoBoardProject internal constructor(
    private val connection: PicoBoardConnection,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
) : AutoCloseable {
    private var lastFrame: PicoBoardFrame? = null

    public val portIdentifier: String
        get() = connection.portIdentifier

    @Throws(PicoBoardException::class)
    public fun update(): PicoBoardProject {
        lastFrame = connection.readFrame()
        return this
    }

    @Throws(PicoBoardException::class)
    public fun values(): PicoBoardScaledValues {
        return ensureFrame().scaled
    }

    @Throws(PicoBoardException::class)
    public fun slider(): Int {
        return values().slider
    }

    @Throws(PicoBoardException::class)
    public fun sound(): Int {
        return values().sound
    }

    @Throws(PicoBoardException::class)
    public fun light(): Int {
        return values().light
    }

    @Throws(PicoBoardException::class)
    public fun buttonPressed(): Boolean {
        return values().buttonPressed
    }

    @Throws(PicoBoardException::class)
    public fun resistanceA(): Int {
        return values().resistanceA
    }

    @Throws(PicoBoardException::class)
    public fun resistanceB(): Int {
        return values().resistanceB
    }

    @Throws(PicoBoardException::class)
    public fun resistanceC(): Int {
        return values().resistanceC
    }

    @Throws(PicoBoardException::class)
    public fun resistanceD(): Int {
        return values().resistanceD
    }

    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun every(
        intervalMillis: Long = connection.options.pollingInterval.toMillis(),
        action: PicoBoardProject.() -> Unit,
    ): Unit {
        require(intervalMillis >= 0L) {
            "intervalMillis must be zero or greater"
        }

        while (true) {
            readFrameForLoop()
            action()
            sleepIfNeeded(intervalMillis)
        }
    }

    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun repeat(
        times: Int,
        intervalMillis: Long = connection.options.pollingInterval.toMillis(),
        action: PicoBoardProject.() -> Unit,
    ): Unit {
        require(times >= 0) {
            "times must be zero or greater"
        }
        require(intervalMillis >= 0L) {
            "intervalMillis must be zero or greater"
        }

        kotlin.repeat(times) { index ->
            readFrameForLoop()
            action()
            if (index < times - 1) {
                sleepIfNeeded(intervalMillis)
            }
        }
    }

    public override fun close() {
        connection.close()
    }

    @Throws(PicoBoardException::class)
    private fun ensureFrame(): PicoBoardFrame {
        val cachedFrame = lastFrame
        if (cachedFrame != null) {
            return cachedFrame
        }
        return update().lastFrame ?: error("PicoBoard frame was not stored")
    }

    @Throws(PicoBoardException::class)
    private fun readFrameForLoop(): PicoBoardFrame {
        var consecutiveFailures = 0
        while (true) {
            try {
                return update().lastFrame ?: error("PicoBoard frame was not stored")
            } catch (failure: PicoBoardException) {
                consecutiveFailures += 1
                if (consecutiveFailures > connection.options.pollingReadFailureRetries) {
                    throw PicoBoardException(
                        "PicoBoard polling failed after ${connection.options.pollingReadFailureRetries} read retries on '$portIdentifier'",
                        failure,
                    )
                }
            }
        }
    }

    private fun sleepIfNeeded(intervalMillis: Long): Unit {
        if (intervalMillis > 0L) {
            sleeper(intervalMillis)
        }
    }
}
