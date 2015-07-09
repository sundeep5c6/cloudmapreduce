This page gives a tutorial on how to write an application in Cloud MapReduce. We use the famous Word Count application as an example. The complete source code is in the repository.

# Base class #

An application should be an inherited class of `MapReduceApp`. The `MapReduceApp` class implements command line arguments parsing, sets up the initial state and inputs, invokes the MapReduce framework, and cleans up after the job is complete. Inheriting from it would make it easy to develop an application, since you can leverage existing code. However, if `MapReduceApp` does not fit your need, you are free to write from scratch.

In Word Count, we declare

```
public class WordCount extends MapReduceApp {
```

# Mapper #

We next need to write the user-defined Map function as a `Mapper` class. All Map functions should implement the `Mapper` interface

```
  public static class Map implements Mapper { 
```

As part of the `Mapper` interface, we must implement the `map` function, which takes the following form:

```
    public void map(String key, String value, 
                    OutputCollector output, PerformanceTracker perf) throws Exception {
```

As described in the original MapReduce paper, the `key` and `value` are both String. The application should convert from the generic String type to the appropriate strong-typed data type. It is in the plan for Cloud MapReduce to eventually support other data types, even custom specified, as both the key and value.

`output` is an `OutputCollector`, which collects the resulting key-value pairs emitted by the Map function and passes them to the reduce stage. Lastly, `perf` is a `PerformanceTracker`, which is a home-grown solution for tracking performance statistics. It can track elapsed time statistics as follows:

```
long startTime = perf.getStartTime();
// some code here, which we want to measure performance
perf.stopTimer("WordCountTime", startTime);
```

You first get the current timestamp into `startTime`, then you let the code you want to measure run. At the end, you invoke `perf.stopTime` which takes two arguments. First, you need to specify a name for the statistics, then you pass in the time recorded at the beginning. `perf` reports the total and average elapsed time for each statistics at the end.

Alternatively, `perf` can track the total number of events as follows:

```
perf.incrementCounter("dataProcessed", data.length());
```

You specify a name for the statistics and an amount to increase. At the end, `perf` reports the total count.

The Map function would parse the `value` for the list of files to process. For each file, it downloads the file from S3, splits the file into words, then invokes the following for each word:

```
output.collect(word, "1");
```

# Reducer #

Next we need to implement the user-defined Reduce function as a `Reducer` class. All Reduce functions must implement the `Reducer` interface.

```
public static class Reduce implements Reducer<int[]> {
```

Cloud MapReduce adopts a slightly different Reduce interface (we call it the Push iterator) than the one used in the original MapReduce paper (we call it the Pull iterator). The Push iterator interface is slightly more efficient. The `Reducer` interface takes a template type as the type used to store intermediate states. As part of the `Reducer` interface, an application must implement the following functions:

```
public int[] start(String key, OutputCollector output) throws IOException {
   return new int[1]; }
```

The `start` function is called exactly once when Cloud MapReduce sees a new reduce key for the first time. This function is typically used to setup initial states. In Word Count, we allocate a new integer variable, which is used to hold the sum. Note that we only need to allocate enough space to hold the state for one reduce key. Also, the state variable has to be non-primitive data type (int, long, byte etc.) so that it can be passed by reference instead of by value. In Word Count, we allocate an integer array with one element instead of one integer.

The `next` function is called once whenever Cloud MapReduce sees a new value for a reduce key. Not only the key and the new value are passed in as parameters, the state variable allocated in the `start` function is also passed in to facilitate state update. Since the state variable is a non-primitive data type, it is passed in by reference and we can update it as needed.

```
public void next(String key, String value, int[] state, OutputCollector output, 
         PerformanceTracker perf) throws IOException {
   state[0] += Integer.parseInt(value); }
```

In Word Count, we convert the value string to its real data type -- integer, then we add it to the state variable.

Next we need to implement the `complete` function which is called exactly once when there are no more values for a reduce key. Here, we perform the necessary clean up, and output the results if needed.

```
public void complete(String key, int[] state, OutputCollector output) throws Exception {
   output.collect(key, String.valueOf(state[0]));  }
```

In Word Count, we convert the total sum stored in the state variable to string, then call the output collector to output the resulting key-value pair.

Lastly, we have to implement an auxiliary function `getSize`, which is used to determine the size of the total state. This is to facilitate memory management. In Word Count, we use only one integer, so we just return 4 bytes.

```
public long getSize(int[] state) throws IOException {
   return 4; }
```

# Invoke MapReduce #

If you inherit from `MapReduceApp`, you need to implement the `run` interface, which is called to initiate the job run.

```
protected void run(String jobID, int numReduceQs,
    int numSetupNodes, SimpleQueue inputQueue, SimpleQueue outputQueue, 
    int numReduceQReadBuffer) throws IOException {
```

The `run` function should initialize the `MapReduce` class, and initialize a new Mapper and Reducer.

```
    MapReduce mapreduce = new MapReduce(jobID, dbManager, queueManager, inputQueue, outputQueue);
    Map map = new Map(s3FileSystem);
    Reduce reduce = new Reduce();
```

Then, `run` should invoke the MapReduce function to start the job processing.

```
    // invoke Map Reduce with input SQS name and output SQS name
    if ( Global.enableCombiner )
	mapreduce.mapreduce(map, reduce, numReduceQs, numSetupNodes, reduce);
    else
        mapreduce.mapreduce(map, reduce, numReduceQs, numSetupNodes, null);
```

Lastly, since the application class is invoked directly, it needs to pass control to the `MapReduceApp` class by calling the `runMain` function.

```
  public static void main(String[] args) throws Exception {
	  new WordCount().runMain(args);
	  System.exit(0);
  }
```

If this section seems complicated, you just need to copy and paste for now. We are looking into ways to make this as painless as possible in the near future.

Congratulations on writing your first Cloud MapReduce application!