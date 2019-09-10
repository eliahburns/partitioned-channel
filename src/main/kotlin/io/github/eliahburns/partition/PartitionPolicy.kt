package io.github.eliahburns.partition

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/** A policy that according to the element, sends that element to a particular channel partition */
typealias PartitionPolicy<E> = (E) -> Int

/** A policy that evenly distributes elements, sending them to the next channel partition in round robin fashion */
class RoundRobinPartitionPolicy<E>(
    private val numPartitions: Int
) : PartitionPolicy<E> {
    private val count = AtomicLong(0)
    override operator fun invoke(e: E): Int = abs(count.incrementAndGet() % numPartitions).toInt()
}


