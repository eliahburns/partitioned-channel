package io.github.eliahburns.channel

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull


class ArrayMultiChannelTest : StringSpec() {


    init {

        "array of channels of 1 should receive send on same channel with rendezvous" {
            ArrayMultiChannel<Int>(capacity = 1, capacityPerChannel = Channel.RENDEZVOUS).use { amc ->
                launch {
                    amc[0].send(0)
                }
                val res = amc[0].receive()
                res shouldBe 0
            }

        }

        "array of channels t 2" {
            ArrayMultiChannel<Int>(capacity = 3, capacityPerChannel = 5).use { amc ->
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

        "array of channels t 3" {
            ArrayMultiChannel<Int>(capacity = 3, capacityPerChannel = 5).use { amc ->
                (0 until 3).forEach { id ->
                    launch {
                        repeat(5) { e ->
                            amc[id].send(e)
                        }
                    }
                }

                val merged = merge(amc)
                withTimeoutOrNull(5.seconds.toMillis()) {
                    for (e in merged) {
                        println(e)
                    }
                }
            }
        }

        "array of channels" {


        }

    }
}