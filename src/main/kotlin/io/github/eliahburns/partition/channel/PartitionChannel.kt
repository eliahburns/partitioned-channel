package io.github.eliahburns.partition.channel

import io.github.eliahburns.partition.*
import io.github.eliahburns.partition.flow.mapAndFlattenToFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


inline fun <E, R> ReceiveChannel<E>.partitionMapAndFlattenToChannel(
    partitions: Int = PartitionedChannel.DEFAULT_PARTITIONS,
    capacity: Int = Channel.RENDEZVOUS,
    context: CoroutineContext = Dispatchers.IO,
    noinline policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(partitions),
    crossinline action: suspend (E) -> R
): ReceiveChannel<MapEither<Throwable, R>> = asPartitionedChannel(partitions, capacity, context, policy)
    .mapAndFlattenToChannel(action)


inline fun <E, R> ReceiveChannel<E>.partitionMapAndFlattenToFlow(
    partitions: Int = PartitionedChannel.DEFAULT_PARTITIONS,
    capacity: Int = Channel.RENDEZVOUS,
    context: CoroutineContext = Dispatchers.IO,
    noinline policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(
        partitions
    ),
    crossinline action: suspend (E) -> R
): Flow<MapEither<Throwable, R>> = asPartitionedChannel(partitions, capacity, context, policy)
    .mapAndFlattenToFlow(action)


@PublishedApi
internal fun <E> ReceiveChannel<E>.asPartitionedChannel(
    partitions: Int = PartitionedChannel.DEFAULT_PARTITIONS,
    capacity: Int = Channel.RENDEZVOUS,
    context: CoroutineContext = Dispatchers.IO,
    policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(partitions)
): PartitionedChannel<E> {
    val partitionedChannel =
        PartitionedChannel(partitions, capacity, context, policy)

    partitionedChannel.launch {
        for (element in this@asPartitionedChannel) {
            partitionedChannel.send(element)
        }
    }

    return partitionedChannel
}


@PublishedApi
internal inline fun <E, R> PartitionedChannel<E>.mapAndFlattenToChannel(
    crossinline action: suspend (E) -> R
): ReceiveChannel<MapEither<Throwable, R>> = produce(context = coroutineContext) {

    for (partition in this@mapAndFlattenToChannel) {
        launch {
            for (element in partition) {
                val result = tryAction(element) { e -> action(e) }
                send(result)
            }
        }
    }
    awaitClose { this@mapAndFlattenToChannel.close() }
}

