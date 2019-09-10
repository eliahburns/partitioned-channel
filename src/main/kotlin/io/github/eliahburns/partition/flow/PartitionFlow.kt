package io.github.eliahburns.partition.flow

import io.github.eliahburns.partition.*
import io.github.eliahburns.partition.channel.mapAndFlattenToChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


inline fun <E, R> Flow<E>.partitionMapAndFlattenToFlow(
    partitions: Int = PartitionedChannel.DEFAULT_PARTITIONS,
    capacity: Int = Channel.RENDEZVOUS,
    context: CoroutineContext = Dispatchers.IO,
    noinline policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(
        partitions
    ),
    crossinline action: suspend (E) -> R
): Flow<MapEither<Throwable, R>> = asPartitionedChannel(partitions, capacity, context, policy)
    .mapAndFlattenToFlow(action)


inline fun <E, R> Flow<E>.partitionMapAndFlattenToChannel(
    partitions: Int = PartitionedChannel.DEFAULT_PARTITIONS,
    capacity: Int = Channel.RENDEZVOUS,
    context: CoroutineContext = Dispatchers.IO,
    noinline policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(
        partitions
    ),
    crossinline action: suspend (E) -> R
): ReceiveChannel<MapEither<Throwable, R>> = asPartitionedChannel(partitions, capacity, context, policy)
    .mapAndFlattenToChannel(action)


@PublishedApi
internal fun <E> Flow<E>.asPartitionedChannel(
    partitions: Int = PartitionedChannel.DEFAULT_PARTITIONS,
    capacity: Int = Channel.RENDEZVOUS,
    context: CoroutineContext = Dispatchers.IO,
    policy: PartitionPolicy<E> = RoundRobinPartitionPolicy(
        partitions
    )
): PartitionedChannel<E> {
   val partitionedChannel =
       PartitionedChannel(partitions, capacity, context, policy)
    this
        .buffer()
        .onEach { partitionedChannel.send(it) }
        .launchIn(partitionedChannel)

    return partitionedChannel
}


@PublishedApi
internal inline fun <E, R> PartitionedChannel<E>.mapAndFlattenToFlow(
    crossinline action: suspend (E) -> R
): Flow<MapEither<Throwable, R>> = channelFlow {

    for (partition in this@mapAndFlattenToFlow) {
        launch {
            for (element in partition) {
                val result = tryAction(element) { e -> action(e) }
                send(result)
            }
        }
    }

    invokeOnClose {
        if (it != null) {
            this@mapAndFlattenToFlow.close()
        } else {
            this@mapAndFlattenToFlow.close(it)
        }
    }
}


