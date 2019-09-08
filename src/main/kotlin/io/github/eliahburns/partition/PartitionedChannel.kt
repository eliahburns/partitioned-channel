package io.github.eliahburns.partition

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow


/**
 * When wishing to parallelize some transformation or action over elements that are received in a [Channel] or [Flow],
 * there may be some level of order that needs to be maintained within subsets of the elements that arrive during and
 * after the transformations/actions are performed.
 *
 * An example could be some *ETL* process in which the source of data provides values (ordered for a given key) for many
 * different keys. If the eventual sink for the elements needs quick delivery and strict time linear ordering of values
 * according to a given key then launching multiple coroutines to perform some action and then loading their result
 * can not guarantee order of delivery.
 *
 * One solution would be to filter at the source and have a separate job for each unique key. But if the number of
 * keys is large and evolves over time this process could be tricky to manage.
 *
 * An alternative solution would be to use some type of partitioning strategy to group the elements from the source into
 * subgroups (partitions) according to their key deterministically, maintain their original ordering, apply the desired
 * transformation or action and then merge the results back together as they are finished, such order is preserved at
 * the partition level.
 *
 * This allows for concurrency and a level of parallelization up to the maximum number of groups or that allowable by
 * hardware architecture the process is running on. This strategy can be useful on top of a partitioning strategy that
 * is already present in a given data source when the sources partition policy is not as fine grained as desired.
 *
 * [PartitionedChannel] allows for this pattern without having to keep track of which [Channel] a given element should
 * be sent to when provided with some lambda or implementation of a [PartitionPolicy].
 * (See [RoundRobinPartitionPolicy] for an example that distributes elements evenly over partitions).
 *
 * When all default values are used, the result is essentially a single [Channel] with [Channel.RENDEZVOUS] capacity. */
@PublishedApi
internal class PartitionedChannel<E>(
    val partitions: Int = DEFAULT_PARTITIONS,
    val capacity: Int = Channel.RENDEZVOUS,
    val context: CoroutineContext = Dispatchers.IO,
    val policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(
        partitions
    )
) : Closeable, Iterable<ReceiveChannel<E>>, CoroutineScope by CoroutineScope(context) {

    private val channels = List(partitions) { Channel<E>(capacity) }

    /** Closes all channels. See [Channel.close] for further details. */
    override fun close() {
        close(null)
    }

    /** Closes all channels. See [Channel.close] for further details. */
    fun close(cause: Throwable?): Boolean = channels.map { it.close(cause) }.all { it }

    /** Adds an [element] into to one of the underlying channels (according to the policy enforced by the
     * [PartitionPolicy], suspending the caller while the buffer of the  channel is full or throws exception if the
     * channel used by the policy is already closed . */
    suspend fun send(element: E) = channels[policy(element)].send(element)

    override fun iterator(): Iterator<ReceiveChannel<E>> = channels.iterator()

    companion object {
        const val DEFAULT_PARTITIONS = 1
    }
}

