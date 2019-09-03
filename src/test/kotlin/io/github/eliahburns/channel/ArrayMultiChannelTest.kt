package io.github.eliahburns.channel

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch


class ArrayMultiChannelTest : StringSpec({
    "array of channels of 1 should receive send on same channel with rendezvous" {
        ArrayMultiChannel<Int>(size = 1, capacity = Channel.RENDEZVOUS).use { amc ->
            launch {
                amc[0].send(0)
            }
            val res = amc[0].receive()
            res shouldBe 0
        }

    }
    "array of channels with capacity of 5 and 3 channels should properly send/receive on each channel" {
        ArrayMultiChannel<Int>(size = 3, capacity = 5).use { amc ->
            (0 until 3).forEach { id ->
                launch {
                    repeat(5) { e ->
                        amc[id].send(e)
                    }
                }
            }

            (0 until 3).forEach { id ->
                eventually(5.seconds) {
                    amc[id].poll()
                    amc[id].poll()
                    amc[id].poll()
                    amc[id].poll()
                    val e = amc[id].poll()
                    e shouldBe 4
                }
            }
        }
    }
})