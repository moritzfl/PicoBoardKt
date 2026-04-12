package de.moritzf.picoboard.easy

import de.moritzf.picoboard.PicoBoardConnection
import de.moritzf.picoboard.PicoBoardException
import de.moritzf.picoboard.PicoBoardOptions
import de.moritzf.picoboard.internal.PicoBoardPacketTransport
import de.moritzf.picoboard.internal.buildPacket
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PicoBoardEasyTest {
    @Test
    fun `sensor getters read one frame and then reuse it`() {
        val transport = FakePacketTransport(
            mutableListOf(
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 100,
                        1 to 200,
                        2 to 300,
                        3 to 1023,
                        4 to 400,
                        5 to 500,
                        6 to 600,
                        7 to 700,
                    ),
                ),
            ),
        )

        createProject(transport).use { project ->
            assertEquals(68, project.slider())
            assertEquals(39, project.resistanceA())
            assertFalse(project.buttonPressed())
            assertEquals(0, transport.remainingResponses())
        }
    }

    @Test
    fun `repeat retries transient read failures`() {
        val transport = FakePacketTransport(
            mutableListOf(
                PicoBoardException("temporary read problem"),
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 100,
                        1 to 200,
                        2 to 300,
                        3 to 0,
                        4 to 400,
                        5 to 500,
                        6 to 600,
                        7 to 700,
                    ),
                ),
                buildPacket(
                    listOf(
                        15 to 4,
                        0 to 101,
                        1 to 201,
                        2 to 301,
                        3 to 1023,
                        4 to 401,
                        5 to 501,
                        6 to 601,
                        7 to 701,
                    ),
                ),
            ),
        )

        val sliderValues = mutableListOf<Int>()
        createProject(
            transport = transport,
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { project ->
            project.repeat(times = 2, intervalMillis = 0) {
                sliderValues += slider()
            }
        }

        assertEquals(listOf(68, 69), sliderValues)
    }

    @Test
    fun `repeat fails after polling retry budget is exhausted`() {
        val transport = FakePacketTransport(
            mutableListOf(
                PicoBoardException("temporary 1"),
                PicoBoardException("temporary 2"),
                PicoBoardException("temporary 3"),
            ),
        )

        createProject(
            transport = transport,
            options = PicoBoardOptions(
                pollingInterval = Duration.ofMillis(5),
                pollingReadFailureRetries = 2,
            ),
        ).use { project ->
            val failure = assertFailsWith<PicoBoardException> {
                project.repeat(times = 1, intervalMillis = 0) { }
            }

            assertEquals(
                "PicoBoard polling failed after 2 read retries on 'fake'",
                failure.message,
            )
        }
    }

    private fun createProject(
        transport: FakePacketTransport,
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardProject {
        return PicoBoardProject(
            connection = PicoBoardConnection(
                transport = transport,
                portIdentifier = "fake",
                options = options,
            ),
            sleeper = { },
        )
    }
}

private class FakePacketTransport(
    private val responses: MutableList<Any>,
) : PicoBoardPacketTransport {
    override val identifier: String = "fake"

    override fun requestPacket(): ByteArray {
        return when (val response = responses.removeFirstOrNull()) {
            null -> throw PicoBoardException("No more packets available")
            is ByteArray -> response
            is PicoBoardException -> throw response
            else -> error("Unsupported fake transport response: ${response::class.qualifiedName}")
        }
    }

    fun remainingResponses(): Int {
        return responses.size
    }

    override fun close(): Unit = Unit
}
