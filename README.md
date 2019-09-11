# Kotlin `PartitionedChannel`

When wishing to parallelize some transformation or action over elements that are received in a `Channel` or `Flow`, 
there may be some level of order that needs to be maintained within subsets of the elements that arrive.

An example could be some *ETL* process in which the source of data provides values (ordered by update arrival to the 
source) for many different keys. If the eventual sink for the elements needs quick delivery and strict time linear 
ordering of values according to a given key, opting to launch multiple coroutines to perform some action and then 
loading their result/s cannot guarantee order of delivery.

One solution would be to filter at the source and have a separate job for each unique key. But if the number of
keys is large and evolves over time this process can be tricky to manage.

An alternative solution would be to use some type of partitioning strategy to group the elements from the source into
subgroups (partitions) according to their key deterministically, maintain their original ordering, apply the desired
transformation or action, and then merge the results back together as they are finished. This allows for concurrency
and a level of parallelization up to the maximum number of partitions or that allowable by the hardware architecture the
process is running on. This strategy can also be useful on top of a partitioning strategy that is already present in a
given data source, when the source's partition policy is not as fine grained as desired.

This library provides the flowing extension methods: 
* `ReceiveChannel<E>.partitionMapAndFlattenToFlow`
* `ReceiveChannel<E>.partitionMapAndFlattenToChannel`
* `Flow<E>.partitionMapAndFlattenToFlow`
* `Flow<E>.partitionMapAndFlattenToChannel`

Each method takes a number of `partitions`, a `capacity` that is applied to each underlying `Channel` partition, a
`context` that defaults to `Dispatchers.IO`, a `policy` that implements some `PartitionPolicy<E>`, and an `action` that
is applied to each element in the original source.


### Examples: 

```kotlin
// decide upon a number of partitions and create a PartitionedMultiChannel
val numPartitions = Runtime.getRuntime().availableProcessors()

// generate a stream of elements and send them through the PartitionedMultiChannel 
val testElements: ReceiveChannel<TestElement> = produceTestElements(10000000000)

// partition up the received elements, take their absolute value in parallel, and then flatten into a flow
testElements
    .partitionMapAndFlattenToFlow(
        partitions = numPartitions,
        policy = RoundRobinPartitionPolicy(numPartitions),
        context = Dispatchers.Default
    ) { it.copy(value = abs(it.value)) }
    .onEach { println(it) }
    .flowOn(Dispatchers.IO)
    .collect()
```

Partition policies can also be combined. For example, if updates to a small subset of the key space are much more 
prevalent than others, their respectful updates can be isolated into their own partitions, while the remainder of
the updates (for any key) can be distributed evenly amongst the other partitions. 

Below illustrates a contrived example of such a policy:

```kotlin
data class KeyValue(val key: String, val value: Double)

// use 20 total partitions
val numPartitions = 20

// initialize a round robin partition policy for 18 of those partitions
val roundRobinPolicy = RoundRobinPartitionPolicy<KeyValue>(numPartitions - 2)

// data class KeyValue(val key: String, val value: Int)
val testElements: ReceiveChannel<KeyValue> = produceTestElements(10000000000)

testElements
    .partitionMapAndFlattenToFlow(
        partitions = numPartitions,
        policy = { element ->
             when(element.key) {
                 "heavily-updated-key-1" -> numPartitions - 1
                 "heavily-updated-key-2" -> numPartitions - 2
                 else -> roundRobinPolicy(element)
             }
        },
        context = Dispatchers.Default
    ) { it.copy(value = abs(it.value)) }
    .onEach { println(it) }
    .flowOn(Dispatchers.IO)
    .collect()
```

### Use

##### Gradle

###### Kotlin DSL
```kotlin
repositories {
    maven(url = "https://dl.bintray.com/eliahburns/maven")
}

dependencies {
    compile("io.github.eliahburns:partitioned-channel:0.1.0")
}
```

###### Groovy DSL
```groovy
repositories {
    maven {
        url  "https://dl.bintray.com/eliahburns/maven" 
    }
}

dependencies {
    compile 'io.github.eliahburns:partitioned-channel:0.1.0'
}
```

##### Maven
```xml
<dependency>
	<groupId>io.github.eliahburns</groupId>
	<artifactId>partitioned-channel</artifactId>
	<version>0.1.0</version>
	<type>pom</type>
</dependency>
```

Overall, this is a pretty slim package, but can be a useful building block within more complex patterns. Pull requests 
for added functionality are welcome.  
