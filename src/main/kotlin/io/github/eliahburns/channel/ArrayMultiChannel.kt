package io.github.eliahburns.channel

import kotlinx.coroutines.channels.Channel
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger


/** An [Array]-like class of iterable and random-accessible [Channel]s containing elements of type [E], in which
 * [size] is an upfront fixed number of channels and [capacity] represents the desired buffer size for each channel. */
class ArrayMultiChannel<E>(
    val size: Int = 1,
    val capacity: Int = Channel.RENDEZVOUS
) : Closeable, Iterable<Channel<E>> {

    private val channels = arrayOfNulls<Channel<E>?>(size)

    init {
        (0 until size).forEach { index -> channels[index] = Channel(capacity) }
    }

    /** Get a channel according to its index from within the underlying array of [Channel]s */
    operator fun get(index: Int): Channel<E> {
        check(index in 0 until size) { "channel index out of range" }
        return channels[index]!!
    }

    /** Close all [Channel]s within the array */
    override fun close() {
        for (channel in this) channel.close()
    }

    /** An [Iterator] over the contained channels beginning at the head of the underlying [Array] */
    override fun iterator(): Iterator<Channel<E>> = object : Iterator<Channel<E>> {
        private var nextIndex = AtomicInteger(0)

        override fun hasNext(): Boolean = nextIndex.get() < size

        override fun next(): Channel<E> {
            check(hasNext()) { "no remaining elements" }
            val nextChannel = channels[nextIndex.get()]!!
            nextIndex.incrementAndGet()
            return nextChannel
        }
    }
}

