This page gives a high level view of the source code structure. It is designed to get a new contributor started quickly. However, keep in mind that, there is no substitute to reading the definitive guide -- the source code.

# Database interface #

Cloud MapReduce uses a central "database" to store the job status. The intent is to have several interfaces, one for each "database" that we support, so that the users can choose the best based on the environment. We currently only support SimpleDB, but the functionalities are well isolated in a single file `com.acnlabs.CloudMapReduce.DbManager` that it should be fairly easy to port to other "databases".

The code in this file is broken down into several well isolated code blocks. The related functions are group close to each other in the source code.

## Node synchronization ##

Nodes can update their status (either "running" or "complete") for each phase (either "setup", "map", "reduce") to the data store. This information is used to synchronize all nodes. Currently, it is only used for synchronizing at the "setup" stage. A few nodes (by default 1) are responsible for creating the reduce queues beforehand, no other nodes should proceed until the setup is finished. The map and reduce stages synchronize using a commit mechanism discussed below.

The following routines are used to update the status to the data store.

```
public void startTask()
public void completeTask()
private void updateStatus()
```

The following routines are used to read the collective states reported by all nodes and determine if a phase is finished.

```
public boolean isPhaseComplete()
public void waitForPhaseComplete()
private int getPhaseSize()
```

## Reduce conflict resolution ##

In rare occasions, two nodes may process the same reduce job at the same time. Eventually, they will realize there is a conflict and will attempt to resolve conflict through the data store.

They use the following routine to report a suspicion that there is a conflict.

```
public void claimReduce()
```

Then, they use the following routine to query the list of potential parties involved in the conflict and resolve to a single owner of the reduce work.

```
public String resolveReduce()
```

## Map and Reduce commit mechanism ##

We use a commit mechanism to guard against failures and to ensure all parts of the job is completed. Whenever a node completes a map or reduce task successfully, it commits the result to the data store by calling the following:

```
public void commitTask()
```

Towards the end of a map or reduce stage, nodes start to query the data store for all completed work by checking the commit messages. They use the following functions and class.

```
private synchronized void insertCommittedTaskWinners()
private class CollectCommittedTaskRunnable implements Runnable
public HashSet<String> getCommittedTask()
public Boolean isStageFinished()
```

The `Runnable` class is used to spawn several threads to query multiple SimpleDB domains in parallel. We use multiple domains to increase the overall write throughput. Beyond simply determining whether all map or reduce work has been completed, Cloud MapReduce also uses `getCommittedTask` to get a list of valid results so that it can filter messages in the queues.

## Determine size of reduce queues ##

We must keep track of how many messages are written to each reduce queue, so that we know how many to expect when we process it. After finishing a map task, the node updates to the data store the number of messages it generated for each reduce queue by calling the following.

```
public void updateReduceOutputPerMap()
```

At the beginning of processing a reduce queue, the node queries the data store to see how many messages to expect. It sums up all values reported by all map nodes by calling the following functions and class.

```
private synchronized void addReduceQSize()
private class CollectReduceQSizeRunnable implements Runnable
public int getReduceQSize()
```

Again, the `Runnable` is used to spawn multiple domains to increase the write throughput.

# Queue interface #

Similar to the database interface, the queue interface is intended to be portable, so that we can run on a different infrastructure than Amazon in the future. Even though today the pure queue processing and the SQS interface parts are mixed in the same file, it is relatively easy to separate out the two functions in the future to support a different queue implementation, such as [ActiveMQ](http://activemq.apache.org/).

`com.acnlabs.CloudMapReduce.QueueManager` contains two queue implementations, both conform to the interface defined by `com.acnlabs.CloudMapReduce.SimpleQueue` and `com.acnlabs.CloudMapReduce.SimpleQueueAdapter`.

One queue implementation is called `SimpleQueueImpl`. It implements the standard Push and Pop interfaces and it interfaces with SQS directly. It contains a variable `maxNumMessages` (configurable by user) which indicates how many SQS messages it attempts to keep in its buffer for reading. If the number of messages falls below the threshold, it spawns many `PopRunnable` objects to download from SQS in parallel. For writing to SQS, it always spawns `PushRunnable` to upload the message to SQS in the background. `SimpleQueueImpl` tags messages before sending them in order to identify which node and which map task generated the messages, and it filters messages when receiving them in order to remove duplicate and redundant messages.

The other queue implementation is called `EfficientQueue`, which is built on top of `SimpleQueueImpl`. It aggregates small messages into a big one before sending it to the underlying `SimpleQueueImpl`. Similarly, it de-multiplex small messages from a big one when reading from `SimpleQueueImpl`.

`QueueManager` also contains several useful functions for creating SQS queues, listing SQS queues so that it knows what additional queue needs to be created, and deleting SQS queues for clean up.

# File system interface #

Cloud MapReduce uses S3 as the file system. Like the database and queue interface, the file system interface could be replaced in the future to support other file systems, such as that provided by [Project Voldemort](http://project-voldemort.com/).

The file system interface is captured in two classes: `com.acnlabs.CloudMapReduce.S3FileSystem` and `com.acnlabs.CloudMapReduce.S3Item`. They are very short, so we do not elaborate further here.

# Cloud MapReduce applications #

There are three sample applications in the `com.acnlabs.CloudMapReduce.application` package: `Grep`, `ReverseIndex`, and `WordCount`. See [the how to write apps tutorial](HowToWriteApp.md) for more understanding of the application code.

Currently, all application code inherits from `MapReduceApp` class. It handles the common tasks such as command line parsing, setting up the environment to launch the job, creating and populating the input queues.

Some more rethinking in this area is needed in terms of what needs to be extracted out for all applications, code refactoring, and how to support cascaded MapReduce jobs.

# MapReduce interfaces #

There are several interfaces defined to ensure a consistent implementation. The `Mapper` and `Reducer` interfaces define how the user-defined map and reduce functions should be written. All Cloud MapReduce jobs must support these interfaces.

In addition, `OutputCollector` defines the standard interface for an application to output the key/value pairs. The `MapCollector`, `CombineCollector` and `ReduceCollector` (defined in `com.acnlabs.CloudMapReduce.mapreduce.MapReduce`) all implements the same interface.

# MapReduce #

`com.acnlabs.CloudMapReduce.mapreduce.MapReduce` is the core and major driver of the MapReduce computation.

It first implements three collector classes: `MapCollector`, `CombineCollector` and `ReduceCollector`.

Each map task instantiates a separate `MapCollector`, which in turn instantiates its own `SimpleQueue` objects and its own thread pool for uploading messages to SQS. This is designed so that we can cleanly flush the output from a map task at the end before declaring the map task is successful and committing the result to the data store. When the `collect` function is called, `MapCollector` applies a hash function to the key to figure out which queue the message should be sent, then it invokes that queue's put interface to send the message. `MapCollector` also keeps track of how many messages are sent to each queue through the collector, and at the end of the map processing, it uploads the counts to the data store.

If combiner is enabled, map outputs are first written to a `CombineCollector`, which are then written to a `MapCollector`. When the `collect` interface is called, `CombineCollector` invokes the user-defined reduce function. It keeps the states associated with all keys in the `combineStates` variable. If the total memory size used exceeds `maxCombineMemorySize` threshold, it invokes the `complete` interface of the user-defined reduce functions and write the results to the `MapCollector`.

The `ReduceCollector` is much simpler. When `collect` is called, it simply outputs the results to the output queue.

The `setup()` function creates all reduce queues ahead of time when the job starts.

The `mapreduce()` function is the main driver of the computation. It consists two stages: map and reduce. In map stage, it dequeues messages from the input queue, and for each message, it spawns a `MapRunnable` to process the map in a separate thread. It makes sure the `mapWorkers` thread pool has empty threads before dequeueing the next map task for processing. When the input queue reports that there are no more messages, it checks with the data store to make sure all map tasks are completed. If not, it goes back to the input queue asking for more work. The reduce stage works in a similar fashion. It checks the master reduce queue for work, spawns `ReduceRunnable` to process a reduce task in a separate thread, and checks the data store at the end to make sure all reduce tasks are completed.

`MapRunnable` simply implements the function to invoke a user-defined map function. The `ReduceRunnable` is a little more involved. First, it queries the data store to determine how many messages are in the reduce queue by summing up the numbers reported by each map task. The total value is used to determine when it has finished processing all messages in the queue. Second, since each reduce queue may contain multiple reduce keys, `ReduceRunnable` keeps a state variable for each reduce key it has seen in `reduceStates` variable. Third, there may be a chance for two nodes to process the same reduce queue at the same time; therefore, `ReduceRunnable` also contains conflict resolution logic to recover from the conflict.

# Supporting classes #

## Thread pool ##

`com.acnlabs.CloudMapReduce.util.WorkerThreadQueue` is our implementation of a thread pool. This class is widely used throughout the code, from threads for map and reduce workers, to queue threads downloading/uploading message from/to SQS in the background.

It contains a buffer which holds `Runnable` objects. It launches a fixed number of threads (specified in the constructor) at the beginning, and these threads are woken up whenever there are new `Runnable` in the buffer. The threads pop `Runnable` from the buffer and start executing them until completion. Users of `WorkerThreadQueue` can `push()` in new `Runnable` for threads to process whenever one of them frees up.

`WorkerThreadQueue` supports a `waitForEmpty()` interface. Caller will go to sleep if all threads are busy, and it is woken up when a thread becomes idle. This interface is used in map and reduce task scheduling to make sure that we do not queue more map/reduce tasks than the threads are able to process. `WorkerThreadQueue` also supports a `waitForFinish()` interface, which is used by queue workers to wait for messages to be completely flushed out.

## Performance tracker ##

`com.acnlabs.CloudMapReduce.performance.PerformanceTracker` is a home-grown solution for tracking performance statistics. It can track elapsed time statistics as follows (`perf` is a `PerformanceTracker` object):

```
long startTime = perf.getStartTime();
// some code here, which we want to measure performance
perf.stopTimer("StatName", startTime);
```

You first get the current timestamp into `startTime`, then you let the code you want to measure run. At the end, you invoke `perf.stopTime` which takes two arguments. First, you need to specify a name for the statistics, then you pass in the time recorded at the beginning. `perf` reports the total and average elapsed time for each statistics at the end.

Alternatively, `perf` can track the total number of events as follows:

```
perf.incrementCounter("dataProcessed", data.length());
```

You specify a name for the statistics and an amount to increase. At the end, `perf` reports the total count.

Once you understand the use cases, it should become apparent what the source code is doing.