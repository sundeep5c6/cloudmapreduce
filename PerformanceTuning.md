# Introduction #

Even though a reasonable default set of parameters are chosen, their choices are unlikely to benefit every application. Depending on the data set, the environment, further performance tuning may be necessary. For example, if you run your applications from outside EC2, you may have a longer latency; thus, you may need more threads to effectively hide the higher latency.

This page gives tips on how to optimize the parameters. In general, you want to make sure the CPU resource and the network resource both achieve high utilization. If CPU is loaded but network is not, and later the two are swapped, then you may be able to squeeze out more performance by parallelizing the two busy periods.

More detailed tips are coming soon.