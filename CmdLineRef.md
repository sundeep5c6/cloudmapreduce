# Command line options #

Cloud MapReduce runs as a stand-alone Java program on the command line. It supports many options to make it easy to specify job parameters and facilitate easy tuning of performance. This page describes the available options.

## Mandatory options ##

The following options are mandatory. They have to be specified on the command line.

  * -k AWS\_ACCESS\_KEY: Specifies the AWS access key. You get the key when you sign up for AWS services.

  * -s AWS\_SECRET\_KEY: Specifies the AWS secret key. The AWS access/secret key pair is used to access S3/SimpleDB/SQS as part of the job run. Make sure you sign up for all three services.

  * -i input\_file: Specifies the input files as path to S3 bucket/folder (e.g., -i /bucket/folder/). It will recursively include all sub-folders and sub-files.

  * -c clientid: Specifies the clientid of a node. A node must have a unique clientid to distinguish the node from other nodes in the system.

  * -j jobid: Even though there is a default ("CloudMapReduce"), you almost always have to specify a unique jobid to avoid conflict. Make sure a jobid is unique, and one is not the prefix of another. A good practice is to name it something like "xxx000", then increment the number part for the next run.

## Optional options ##

These options all have a default value. You only need to specify them if you want to override the defaults.

  * -p x: Default=1. The number of partitions the input files should be split into. The input files are split into equal partitions to facilitate parallel processing. Even though optional, this option probably should always be set to facilitate parallel processing.

  * -r x: Default=1. The number of reduce partitions the intermediate results should be split into. The more reduce partitions, the finer granularity of work allocation. Even though optional, this option probably should always be set to facilitate parallel processing.

  * -tm x: Default=2. Specifies the number of local Map threads. Threads are used to parallelize map processing in order to hide S3 and SQS download latency.

  * -tr x: Default=4. Specifies the number of local Reduce threads. Threads are used to parallelize reduce processing in order to hide S3 and SQS download latency.

  * -tu x: Default=20. Specifies the number of upload workers for each Map task. Each Map generates many key-value pairs, which are uploaded to SQS. Threads help to hide SQS upload latency.

  * -td x: Default=20. Specifies the number of download workers for each Reduce task. Each Reduce task reads many key-value pairs, which are downloaded from a reduce queue in SQS. Threads help to hide SQS download latency.


  * -b x: Default=100. Specifies the number of messages to buffer when reading from a reduce queue. A number of threads (specified by the -td option) run in parallel to download messages from reduce queues if the number of reduce messages in the buffer falls below this threshold.

  * -n x: Default=1. The number of nodes that should participate in reduce queue creation at the beginning of the job run. A reduce queue is created for each reduce partition. This should stay at the default most of the time, unless the number of reduce partitions is huge.

  * -m x: Default=1. The number of SimpleDB domains to use to spread out the load. Only needed when there are a large number of nodes. As a reference, we used 50 domains when running with 1000 nodes. How many domains to use also depends on the number of tasks and the task granularity.

  * -o string: Default="outputqueue". The output queue name where the result is held. Queue name is prepended with the jobid, i.e., jobid\_outputqueue.

  * -cb: Default=combiner disabled. When specified, it enables combiner.

  * -d x: Default=0. For debug purpose, you can print out x number of outputs at the end of computation. The outputs you print are gone from the output queue for a duration of the visibility timeout.

  * -vi x: Default=7200. The visibility timeout for the input queue -- time for an input queue message to reappear.

  * -vr x: Default=600. The visibility timeout for the reduce queues. The reduce queues hold the intermediate results. It affects the time taken for conflict resolution in the reduce phase.

  * -vm x: Default=7200. The visibility timeout for the master reduce queue, which holds the set of reduce partitions.