# Introduction #

Generic roadmap, for collaborative discussion.

# Details #

  * Test cases/Continuous integration (possibly migrate to Maven?)
    * Status: Done with Maven integration. Working on a e2e integration test.
  * Junit test framework. Need a framework and test coding standard to support Junit tests for all modules in the future.
  * More sample MapReduce applications to show people how to use it. As an idea, [Phoenix](http://mapreduce.stanford.edu) implemented a few more, including
    * MatrixMultiply
    * KMeans
    * PCA
    * Histogram
    * LinearRegression
> See their paper and code for more details.

  * Support for cascaded MapReduce
  * Rethink the MapReduceApp class to make it easier for people to write applications
  * Performance optimization (code never gone through a profiler yet)
  * Cost optimization in DBManager. Look for ways to minimize database queries to reduce SimpleDB cost. The current architecture is that all nodes write single entry status report, then run "select" query at the end. While waiting for other nodes, the "select" query could be run many times, increasing the SimpleDB cost. Instead of running "select", we should be able to just query the missing nodes' status.
  * Visual monitoring tool to watch job progress. (All statuses are in SimpleDB, this tool just need to query SimpleDB to get a clear view). Would be great for a demo and monitoring purpose to see on a GUI that statuses are being updated as job progresses.
  * Clearly separate out the cloud dependent stuff from non-cloud-dependent stuff, to make it easy to support other cloud platforms in the future.
  * Support an internal cloud instantiation. Need to lock in a platform to be based on. Possible choices are ActiveMQ, Voldemort, CouchDB, Mnesia, MongoDB, Neo4J, Tokoyo Cabinet
  * Tools to export data in a user-friendlier form. All outputs are currently in SQS, need to put them in a file?
  * Support more data types other than strings for key/value pairs.