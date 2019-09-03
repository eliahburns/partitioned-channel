package io.github.eliahburns.channel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import java.lang.RuntimeException
import kotlin.random.Random


/** A simple test to check for handling of mapping successes/failures and analysis with VisualVM */
fun main() = runBlocking {
    println("starting")
    val numPartitions = 3
    val partitionedChannels = PartitionedMultiChannel(
        totalPartitions = numPartitions,
        partitionCapacity = Channel.BUFFERED,
        sorter = object : PartitionPolicy<TestElement> {
            override fun invoke(p1: TestElement): Int = p1.group % numPartitions

        }
    )

    val testElements = produceTestElements(10000000000)


    launch(Dispatchers.IO) {
        for (testElement in testElements) {
            partitionedChannels.send(testElement)
        }
    }

    launch(Dispatchers.IO) {
        val merged = mapAndMergePartitions(
             partitions = partitionedChannels,
            capacity = 1_000
        ) {
            if (Random.nextBoolean()) {
                it.copy(value = it.value * it.value)
            } else {
                throw RuntimeException("failed to transform value")
            }
        }
        for (mapped in merged) {
            println("${Thread.currentThread().name}: mapped element after merge: $mapped")
        }
    }

    delay(50000)
    cancel()
    Unit
}

fun KeySet(
    numKeys: Long = 100
) = (0 until numKeys).toSet()

fun CoroutineScope.produceTestElements(numElements: Long = 100, keySet: Set<Long> = KeySet()) = produce(
    capacity = 1000
) {

    for (value in 0 until numElements) {
        val nextKey = keySet.random()
        val elem = TestElement(nextKey, value, (nextKey % keySet.size).toInt())
        send(elem)

    }
    close()
}

data class TestElement(val key: Long, val value: Long, val group: Int)

