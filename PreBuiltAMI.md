# AMI image #

To make running a Cloud MapReduce job easier, we have built a AMI image which hides a lot of details. To launch a job, specify how many EC2 instances you need, and specify parameters (described below) as user data, then you are in business.

AMI ID: ami-d29775bb, 32bits, x86, US (have not tried EU, probably does not work)

The AMI ID may change over time as we rebundle the image to fix problems, but the manifest will stay the same. You will always find the latest in S3 as huanliu/CloudMapReduce.manifest.xml.


# How to use the AMI #

The AMI takes configuration data as user data in the following form.

```
AWS_ACCESS_KEY=xxxxxxxxxx
AWS_SECRET_KEY=ssssssssssssssssssss
CLOUDMAPREDUCE_JAR=/mybucket/cloudmapreduce-api-0.7-release.jar,/mybucket/cloudmapreduce-examples-0.7-release.jar
COMMAND_LINE="com.acnlabs.CloudMapReduce.application.WordCount -k xxxxxxxxxx -s ssssssssssssssssssss -i /cloudos/test25.txt -j jobid -p 20 -r 20 -cb -c CLIENT_ID"
JOB_ID=test100
OUTPUT=/mybucket/results
OTHER_CONFIG=/mybucket/config
OTHER_SCRIPT=/mybucket/script
BASE_ID=0
```

The user data is dumped to a file, and source'd as a bash shell file. In essence, it defines a set of shell variables. The meaning of each is as follows.

  * AWS\_ACCESS\_KEY:  This is your AWS access key. You get this key when you sign up for AWS services.

  * AWS\_SECRET\_KEY:  This is your AWS secret key. The access/secret key pair is used to download JAR file, upload log file, so make sure they are valid for the buckets specified.

  * CLOUDMAPREDUCE\_JAR: This is a comma-separated list of JAR files which contain both Cloud MapReduce and your application. You can either do "mvn install" in the source tree to compile it into JARs (one API jar, one examples jar), or use "Export->JAR file" in Eclipse. The first part of the path should be the S3 bucket name. In the example, "mybucket" is the bucket name. You do not need to upload standard libraries (S3, SQS, SimpleDB, Jakarta, JAXB, args4j, log4j) as they are included in the image already. But make sure you upload both the API (the MapReduce logic) and your own application.

  * COMMAND\_LINE: This is the command line to run on each instance. See the [Command Line Reference](CmdLineRef.md) for details. Note that you will need to provide an AWS access/secret key pair on the command line, and it is used to access SQS/S3/SimpleDB services as part of the Cloud MapReduce job. Also, "CLIENT\_ID" is a special variable, which will be translated to a numeral number that will be supplied as the "clientid" option on the command line. Each node needs to have a unique "clientid".

  * JOB\_ID: A name used to create a subfolder to store the resulting log file.

  * OUTPUT: The top level directory to store log files. The first path (e.g., "mybucket" in the example) should be a bucket name. The log files are stored under /OUTPUT/JOB\_ID/reservation\_id/launch\_id.

  * OTHER\_CONFIG: The configuration data in user data is fixed once an instance is launched. To facilitate multiple job runs per launch, you can optionally specify an additional configuration file in S3. The script reads the configuration data in the user data first, then download the OTHER\_CONFIG data. In other words, configuration data in OTHER\_CONFIG overwrites what is in the user data. You can specify a different job parameter, and reboot the instances to start processing. Rebooting is more cost effective since AWS rounds up your usage to an hour.

  * OTHER\_SCRIPT: The AMI has a built-in script to launch jobs. But, you can optionally include an additional script to change its behavior. OTHER\_SCRIPT is a bash script, and it is called right before invoking the Cloud MapReduce command line.

  * BASE\_ID: Each node needs to have a unique clientid. If you specify "-c CLIENT\_ID" on the command line parameter, the clientid is determined by summing BASE\_ID with the launch ID. BASE\_ID by default is 0. When you first launch a set of nodes, each will have a unique launch ID. For example, if you launch 100 instances, they are numbered 0 to 99. But if you launch additional instances later to help with the processing, you need to set the BASE\_ID to be more than 100 for the new set of nodes, so that the new nodes would have a unique clientid. BASE\_ID should not be set in OTHER\_SCRIPT if you launched two batches of instances, since each batch will use the same BASE\_ID, and it will result in duplicate clientid.

# login #

To login to an instance of this AMI, you can ssh into the "cloudmapreduce" account using your key-pair. Instead of waiting for the job finish to see the log, you can watch the log file as the job progresses. Go to the home (~) directory, then type "tail -f output".

# Visual monitoring #

The AMI automatically updates machine statistics, including CPU, network, disk, and memory usage, to Google App Engine (GAE). You can monitor these statistics visually at http://ec2-performance.appspot.com. Input the DNS name of your EC2 instance, e.g., ec2-174-129-44-38.compute-1.amazonaws.com, then click `graph`. You will see something similar to the following

http://s3.amazonaws.com/huanliu/GAEmonitoring.JPG

Since GAE is a free service, there is a limit on the amount of data it can hold. If your data is not displaying, you may need to click `delete` to clear the data store. Either input a DNS name, which will only clear data related to that instance, or leave the name empty, which will clear the whole data store. Obviously, multiple users could step on each other's toes, but this is only for testing usage. For production use, we can help to put in a scalable solution.