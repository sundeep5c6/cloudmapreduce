Cloud MapReduce was initially developed at Accenture Technology Labs. It is a MapReduce implementation on top of the Amazon Cloud OS.

Cloud MapReduce has minimal risk w.r.t. the [MapReduce patent](http://huanliu.wordpress.com/2010/01/22/googles-mapreduce-patent-and-its-impact-on-hadoop-and-cloud-mapreduce/), compared to other open source implementations, as it is implemented in a completely different architecture than described in the Google paper.

By exploiting a cloud OS's scalability, Cloud MapReduce achieves three primary advantages over other MapReduce implementations built on a traditional OS:

  * It is faster than other implementations (e.g., 60 times faster than Hadoop in one case. Speedup depends on the application and data.).

  * It is more scalable and more failure resistant because it has no single point of bottleneck.

  * It is dramatically simpler with only 3,000 lines of code (e.g., two orders of magnitude simpler than Hadoop).

See details in [Cloud MapReduce paper](http://sites.google.com/site/huanliu/ccgrid11.pdf).

See the [Tutorial](Tutorial.md) to learn how to use Cloud MapReduce and how easy it is to launch a job. See the [how to guide](HowToWriteApp.md) on how to write your first Cloud MapReduce application. If you want to contribute to the project or if you want to port Cloud MapReduce to other clouds (e.g., Windows Azure or an internal cloud), see the [code architecture](CodeArchitecture.md) page to learn how it is designed.

Also see [Command line options](CmdLineRef.md) for details on how to specify a job run,  [Pre-built AMI](PreBuiltAMI.md) for how to use the pre-built AMI image to make running the job easier, and [performance tuning](PerformanceTuning.md) page for tips to optimize the performance.

Cloud MapReduce is at its early stage. We welcome and appreciate your contributions. We have exciting plans to support multiple clouds and to make the system more robust/easy to use. Please join the [mailing list](http://groups.google.com/group/CloudMapReduce) to see where you can contribute and post any questions (either development coordination or questions on how to use) to the developers.