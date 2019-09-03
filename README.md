# Kotlin Partitioned Multi-Channel

When wishing to parallelize some transformation or action over elements that are received in a channel, there may
be some level of order that needs to be maintained within subsets of the elements that arrive.

An example could be some *ETL* process in which the source of data provides values (ordered by update arrival to the 
source) for many different keys. If the eventual sink for the elements needs quick delivery and strict time linear 
ordering of values according to a given key then launching multiple coroutines to perform some action and then load 
their result cannot guarantee order of delivery.

One solution would be to filter at the source and have a separate job for each unique key. But if the number of
keys is large and evolves over time this process can be tricky to manage.

An alternative solution would be to use some type of partitioning strategy to group the elements from the source into
subgroups (partitions) according to their key deterministically, maintain their original ordering, apply the desired
transformation or action and then merge the results back together as they are finished. This allows for concurrency
and a level of parallelization up to the maximum number of groups or that allowable by the hardware architecture the
process is running on. This strategy can be useful on top of a partitioning strategy that is already present in a
given data source when the sources partition policy is not as fine grained as desired.

**PartitionedMultiChannel** combined with **mapAndMergePartitions** allows for this pattern without having to keep track
of which **Channel** a given element should be sent to when provided with some lambda or implementation of a
**PartitionPolicy** (See **RoundRobinPartitionPolicy** for an example).


## Example usage: 

```kotlin
// decide upon a number of partitions and create a PartitionedMultiChannel
val numPartitions = 100

val partitionedChannel = PartitionedMultiChannel<TestElement>(
    totalPartitions = numPartitions,
    partitionCapacity = Channel.BUFFERED) { element ->
    // decide upon a partitioning policy
    element.key.hashCode() % numPartitions
}


launch(Dispatchers.IO) {
    // generate a stream of elements and send them through the PartitionedMultiChannel 
    val testElements = produceTestElements(10000000000)
   
    for (testElement in testElements) {
        partitionedChannel.send(testElement)
    }
}

launch(Dispatchers.IO) {
    val merged = mapAndMergePartitions(
         partitions = partitionedChannel,
        capacity = 1_000
    ) { elementToMap ->
        // multiply its value by itself and copy remaining parameters
        elementToMap.copy(value = elementToMap.value * elementToMap.value)
    }
}

for (mapped in merged) {
    println("mapped element after merge: $mapped")
}
```
