package io.github.eliahburns.channel

import io.github.eliahburns.channel.util.produceTestElements
import io.github.eliahburns.partition.RoundRobinPartitionPolicy
import io.github.eliahburns.partition.channel.partitionMapAndFlattenToFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {

    val testElements = produceTestElements(10000000000)

    val numPartitions = 1000

    testElements
        .partitionMapAndFlattenToFlow(
            partitions = numPartitions,
            policy = RoundRobinPartitionPolicy(numPartitions)
        ) { it }
        .onEach { println(it) }
        .flowOn(Dispatchers.IO)
        .collect()

    delay(10_000)
}
