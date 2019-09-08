package io.github.eliahburns.channel.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce

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

