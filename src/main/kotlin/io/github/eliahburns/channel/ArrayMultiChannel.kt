package io.github.eliahburns.channel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import java.io.Closeable


class ArrayMultiChannel<E>(
    val capacity: Int,
    val capacityPerChannel: Int
) : Closeable {

    private val channels = arrayOfNulls<Channel<E>?>(capacity)

    init {
        (0 until capacity).forEach { id -> channels[id] = Channel(capacityPerChannel) }
    }

    operator fun get(id: Int): Channel<E> {
        check(id in 0 until capacity) { "channel id out of range" }
        return channels[id]!!
    }

    override fun close() {
        (0 until capacity).forEach { id -> channels[id]?.close() }
    }
}


fun <E> CoroutineScope.merge(
    channels: ArrayMultiChannel<E>
): ReceiveChannel<E> = produce {
    repeat(channels.capacity) { id ->
        launch {
            for (e in channels[id]) {
                send(e)
            }
        }
    }
}

fun <E, R> CoroutineScope.merge(
    channels: ArrayMultiChannel<E>,
    action: (E) -> R = { e -> e as R }
): ReceiveChannel<R> = produce {
    repeat(channels.capacity) { id ->
        launch {
            for (e in channels[id]) {
                val r = action(e)
                send(r)
            }
        }
    }
}


