package io.github.eliahburns.channel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.atomic.AtomicLong


/**
 * When wishing to parallelize some transformation or action over elements that are received in a channel, there may
 * be some level of order that needs to be maintained within subsets of the elements that arrive.
 *
 * An example could be some *ETL* process in which the source of data provides values (ordered for a given key) for many
 * different keys. If the eventual sink for the elements needs quick delivery and strict time linear ordering of values
 * according to a given key then launching multiple coroutines to perform some action and then load their result cannot
 * guarantee order of delivery.
 *
 * One solution would be to filter at the source and have a separate job for each unique key. But if the number of
 * keys is large and evolves over time this process can be tricky to manage.
 *
 * An alternative solution would be to use some type of partitioning strategy to group the elements from the source into
 * subgroups (partitions) according to their key deterministically, maintain their original ordering, apply the desired
 * transformation or action and then merge the results back together as they are finished. This allows for concurrency
 * and a level of parallelization up to the maximum number of groups or that allowable by hardware architecture the
 * process is running on. This strategy can be useful on top of a partitioning strategy that is already present in a
 * given data source when the sources partition policy is not as fine grained as desired.
 *
 * [PartitionedMultiChannel] combined with [mapAndMergePartitions] allows for this pattern without having to keep track
 * of which [Channel] a given element should be sent to when provided with some lambda or implementation of a
 * [PartitionPolicy] (See [RoundRobinPartitionPolicy] for an example).
 *
 * When all default values are used, the result is essentially a single [Channel] with [Channel.RENDEZVOUS] capacity. */
class PartitionedMultiChannel<E>(
    val totalPartitions: Int = DEFAULT_TOTAL_GROUPS,
    val partitionCapacity: Int = Channel.RENDEZVOUS,
    val sorter: PartitionPolicy<E> = RoundRobinPartitionPolicy(totalPartitions)
) : Closeable, Iterable<Channel<E>> {

    private val channels = ArrayMultiChannel<E>(size = totalPartitions, capacity = partitionCapacity)

    /** Closes all channels. See [Channel.close] for further details. */
    override fun close() {
        close(null)
    }

    /** Closes all channels. See [Channel.close] for further details. */
    fun close(cause: Throwable?): Boolean = channels.map { it.close(cause) }.all { it }

    /** Registers handler which is synchronously invoked once the underlying channels are closed */
    @ExperimentalCoroutinesApi
    fun invokeOnClose(handler: (cause: Throwable?) -> Unit) = channels.map { it.invokeOnClose(handler) }
        .reduce { u: Unit, u1: Unit -> Unit }

    /** Adds [element] into to one of the underlying channels (according to the policy enforced by the [PartitionPolicy]
     * if it is possible to do so immediately without violating capacity restrictions and returns `true`. Otherwise, it
     * returns `false` immediately or throws exception if the channel [isClosedForSend]. */
    fun offer(element: E): Boolean = channels[sorter(element)].offer(element)

    @ExperimentalCoroutinesApi
    val isClosedForSend: Boolean get() = channels.map { it.isClosedForSend }.all { it }


    /** Adds an [element] into to one of the underlying channels (according to the policy enforced by the
     * [PartitionPolicy], suspending the caller while the buffer of the  channel is full or throws exception if the
     * channel [isClosedForSend]. */
    suspend fun send(element: E) = channels[sorter(element)].send(element)

    override fun iterator(): Iterator<Channel<E>> = channels.iterator()

    companion object {
        const val DEFAULT_TOTAL_GROUPS = 1
    }
}

/** Maps the elements within a [PartitionedMultiChannel] and merges them into a single [ReceiveChannel] while preserving
 * linear order of elements according to how they where originally partitioned. */
@ExperimentalCoroutinesApi
inline fun <E, R> CoroutineScope.mapAndMergePartitions(
    partitions: PartitionedMultiChannel<E>,
    capacity: Int = Channel.RENDEZVOUS,
    crossinline map: (E) -> R
): ReceiveChannel<MapAndMergeResult> = produce(
    capacity = capacity,
    context = coroutineContext
) {
    for (channelPartition in partitions) {
        launch {
            for (element in channelPartition) {
                val result = try {
                    MapAndMergeResult.Success(map(element))
                } catch (e: Exception) {
                    MapAndMergeResult.Failure(element, e)
                }
                send(result)
            }
        }
    }
}

/** A policy that according to the element, sends that element to a particular channel partition */
typealias PartitionPolicy<E> = (E) -> Int

/** A policy that evenly distributes elements, sending them to the next channel partition in round robin fashion */
class RoundRobinPartitionPolicy<E>(
    private val numPartitions: Int
) : PartitionPolicy<E> {
    private val count = AtomicLong(0)
    override operator fun invoke(e: E): Int = (count.incrementAndGet() % numPartitions).toInt()
}

/** The result returned for each element after applying [mapAndMergePartitions] to the elements from a
 * [PartitionedMultiChannel] */
sealed class MapAndMergeResult {

    data class Success<R>(
        val value: R
    ) : MapAndMergeResult()

    data class Failure<E>(
        val value: E,
        val cause: Throwable
    ) : MapAndMergeResult()
}
