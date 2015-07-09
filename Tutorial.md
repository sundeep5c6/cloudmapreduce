# Tutorial: how to run your first Cloud MapReduce job #

This tutorial covers how to run an existing sample application in Cloud MapReduce. We cover the details on how to write an application for Cloud MapReduce in [the how to write application tutorial](HowToWriteApp.md).

First, let us get a copy of the source code. The easiest way is to download a release, e.g., CloudMapReduce-0.7-release.zip. Unzip it to a directory, e.g., "/workspace/cloudmapreduce".

Alternatively, you can check out from the source code repository, which always has the latest fixes compared to a released version. Run

```
svn checkout http://cloudmapreduce.googlecode.com/svn/trunk/ cloudmapreduce
```

`svn` is the subversion client program used for checking out source code. If you are on a Windows platform, you may need to use [Cygwin](http://www.cygwin.com) to get the `svn` command. Or you can use a graphical interface, such as [Tortoise](http://tortoisesvn.tigris.org/).

Once checked out, go into the main directory and compile the source code.

```
cd cloudmapreduce
mvn install -DskipTests
```

Maven (`mvn`) is a build compilation system. You can get it from [Maven download](http://maven.apache.org/download.html), or you can use Eclipse's export feature to compile it.

We've working to include some automated testing behind the codebase.  As you can imagine the dependencies on AWS make a bit complicated.

The Integration test requires that you've set your AWS API info as environment variables - something like this in bash:

```
export AMAZON_ACCESS_KEY_ID='abcdefghijklmnop'
export AMAZON_SECRET_ACCESS_KEY='1234567891012345'
```

Then, to run the integration test (a clone of the WordCount example):

```
mvn test
```

or to test then install

```
mvn install
```

You should now have a cloudmapreduce-api-....jar in cloudmapreduce-api/target directory and a cloudmapreduce-examples-....jar in cloudmapreduce-examples/target directory (they are in the root directory in a release zip file). These are the executable files we will run. You can run it anywhere. In fact, you can run it in the SETI@HOME style, where you harvest idle computing cycles. A node can join the computation any time, and it can automatically figure out the overall job status and join the computation, but that is a more advanced topic. You can also run it from EC2 manually, but we will describe an easier way -- using a pre-built AMI -- to run it in EC2 in the following first.

# Run Cloud MapReduce job using the pre-built AMI #

Let us first upload the executable files to a bucket in S3. There are a variety of tools to use, I use [S3fox](https://addons.mozilla.org/en-US/firefox/addon/3247), which is a plugin for the firefox browser. Assume we put them under S3 bucket `mybucket`.

Now we can launch instances in EC2 to start the job. Again, you can use a variety of tools, but I use [ElasticFox](http://developer.amazonwebservices.com/connect/entry.jspa?externalID=609), a plugin for firefox. The following picture shows the steps to use with ElasticFox. (note the AMI ID shown is out of date).

http://s3.amazonaws.com/huanliu/LaunchCloudMapReduce.JPG

  1. Find the image we built. Find image `ami-d29775bb`. The AMI id may change when we rebundle the image, but the manifest will always be `huanliu/CloudMapReduce.manifest.xml`.
  1. Click the launch button to start instances
  1. On the launch page, specify the number of instances you need. There is no complex cluster setup needed, just launch them and they will process in parallel.
  1. Set the user data. The user data specifies the job parameters. We will discuss the parameter shortly.
  1. Click the launch button. Your instances will boot up, and as soon as they are up, they start processing your job.

The user data we submitted in step 4 looks like the following:

```
AWS_ACCESS_KEY=AAAAAAAA
AWS_SECRET_KEY=SSSSSSSSSSSSSSSS
CLOUDMAPREDUCE_JAR=/mybucket/cloudmapreduce-api-0.7-release.jar,/mybucket/cloudmapreduce-examples-0.7-release.jar
COMMAND_LINE="com.acnlabs.CloudMapReduce.application.WordCount -k AAAAAA -s sssssss -i /cloudos/test25.txt -j job001 -p 20 -r 20 -cb -c CLIENT_ID"
JOB_ID=test100
OUTPUT=/mybucket/results
OTHER_CONFIG=/mybucket/config
BASE_ID=0
```

The meaning of these parameters are discussed in more detail in the [pre-built AMI](PreBuiltAMI.md) page, but I will touch upon them briefly.

`AWS_ACCESS_KEY` and `AWS_SECRET_KEY` are your AWS credential. They are used to download the executable file and upload the log files.

`CLOUDMAPREDUCE_JAR` is a list of comma-separated executable JAR files in S3. Just point to the location where we have uploaded them earlier. `OUTPUT` and `JOB_ID` specifies the location in S3 where to update the log file at the end. They are uploaded to `/OUTPUT/JOB_ID/reservationId/launchId`.

`OTHER_CONFIG` is used to specify a new set of job parameters, so that you can simply reboot your instances for the next job, since user data cannot be changed once launched. This is more cost effective than launching new instances. Another parameter, `OTHER_SCRIPT`(not used in the example), is designed for you to customize our script, but it is more advanced.

Each instance in your Cloud MapReduce job needs to have a unique `clientid`. `BASE_ID` is used to determine the starting id number instances should be assigned. It is useful when you launch multiple batch of instances to avoid conflict.

`COMMAND_LINE` specifies the command that will run on the command line when invoking the executable. Seem [Command Line Reference](CmdLineRef.md) for the full set of supported parameters. The parameter used in the example are:

  * com.acnlabs.CloudMapReduce.application.WordCount: The first one specifies the Java class to run. In this example, let us just run the famous Word Count application.

  * -k AAAAAA -s sssssss: These specifies the AWS credential to use. They are used to access SimpleDB/SQS/S3 from the executable.

  * -i /mybucket/test25.txt: This specifies the input file(s). If you list a directory, all files under the directory are processed. In this example, make sure test25.txt exists in bucket `mybucket`.

  * -j job001: This specifies the job id. Each job must have a unique job id, to avoid stepping on each other's toe. If you see lots of duplicate messages or conflict resolutions, you probably reused a job id. MAKE SURE that the job id for each run is different and one is not the prefix of another.

  * -p 20: Splits the input file(s) into 20 equal portions.

  * -r 20: Splits the reduce phase into 20 equal partitions.

  * -cb:  Enable combiner

  * -c `CLIENT_ID`: Assign a `clientid` to each node. `CLIENT_ID` is translated into different number for different nodes launched in the same batch. It is set to `BASE_ID`+launchId for an instance.

At the end of the job, you can look in the S3 bucket to see the log file and see whether it has finished successfully. Alternatively, you can login to any instance, using the `cloudmapreduce` account with your key-pair, and watch the log file `~/output` for on-the-fly update.

When the job finishes, all results are stored in the output queue. By default, it is `jobid_outputqueue`. There are scripts in the AMI to help query the queue. For example, `~/deq.py` can dequeue messages from any queue, `~/deleteQ.py` can delete all queues if a previous run did not clean up properly.

The AMI automatically updates machine statistics, including CPU, network, disk, and memory usage, to Google App Engine (GAE) (See [Pre-built AMI](PreBuiltAMI.md) for more details]. You can monitor these statistics visually at http://ec2-performance.appspot.com. Input the DNS name of your EC2 instance, e.g., ec2-174-129-44-38.compute-1.amazonaws.com, then click `graph`. You will see something similar to the following:

http://s3.amazonaws.com/huanliu/GAEmonitoring.JPG

# Run Cloud MapReduce job using command line #

Alternatively, you can run Cloud MapReduce jobs locally on your own machine. This is used more often when you debug and test your changes. You can use the shell script `bin\run` in the source code, and provide the command line argument, i.e.,

```
bin/run com.acnlabs.CloudMapReduce.application.WordCount -k AAAAAA -s sssssss -i /cloudos/test25.txt -j job001 -p 20 -r 20 -cb -c 0
```

`bin/run` is nothing but a Java command specifying all locations of the libraries:

```
java -Xmx1500m -classpath .:cloudmapreduce-api-0.8-development.jar:cloudmapreduce-examples-0.8-development.jar:lib/amazon-queue-2008-01-01-java-library.jar:lib/jakarta-commons/commons-httpclient-3.0.1.jar:lib/jakarta-commons/commons-codec-1.3.jar:lib/jakarta-commons/commons-logging-1.1.jar:lib/log4j/log4j-1.2.14.jar:lib/amazon-simple-db-2009-04-15-java-library.jar:lib/args4j-2.0.12.jar:lib/s3.jar $@
```

If you have problems running the command, most likely it is because you are missing some dependent Jar files. Specify them on the command line will fix the problem.

Within Eclipse, you can specify the command line arguments in the launch configuration. You do not need to specify the dependent library as it is automatically taken care of by Eclipse
Congratulations on your first Cloud MapReduce job.