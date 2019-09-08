package io.github.eliahburns.channel

import io.github.eliahburns.channel.util.produceTestElements
import io.github.eliahburns.partition.RoundRobinPartitionPolicy
import io.github.eliahburns.partition.channel.partitionMapAndFlattenToChannel
import kotlinx.coroutines.*
import java.lang.RuntimeException
import kotlin.random.Random


/** A simple test to check for handling of mapping successes/failures and analysis with VisualVM */
fun main() = runBlocking {
    println("starting")
    val numPartitions = 100

    val testElements = produceTestElements(10000000000)

    val flattened = testElements
        .partitionMapAndFlattenToChannel(
            partitions = numPartitions,
            policy = RoundRobinPartitionPolicy(numPartitions)
        ) { elementToMap ->
            if (Random.nextBoolean()) {
                // multiply its value by itself
                elementToMap.copy(value = elementToMap.value * elementToMap.value)
            } else {
                // randomly fail
                throw RuntimeException("failed to transform value")
            }
        }

    launch(Dispatchers.IO) {
        for (elem in flattened) {
            println(elem)
        }
    }

    delay(50000)
    cancel()
    Unit
}

