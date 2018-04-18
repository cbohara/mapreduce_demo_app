Demo app and notes from 
Hadoop: The Definitive Guide
by Tom White

#####################################
Ch 1 - Meet Hadoop
#####################################

data storage and analysis
	storage capacities of hard drives have increased massively over the years
	but access speeds — the rate at which data can be read from drives — have not kept up
	it takes a long time to read and write data to a single drive

	reduce time by reading from multiple disks at once 
		ex: 100 drives each hold 1/100th of the data
		work in parallel to read the data quickly

	avoid data loss
		many pieces of hardware > greater risk of hardware failure
		avoid data loss through replication

	combine data from many disks
		MapReduce = programming model
			abstracts the problem from disk reads and writes 
			transforms to computation over sets of keys and values

	hadoop provides
		reliable, scalable platform for storage and analysis
		runs on commodity hardware
		open source

	MR = batch query processer
		provides ability to run ad hoc query against entire data set
		get results in a reasonable time
		still, emphasis on batch processing and not for interactive analysis

beyond batch
	Hadoop ecosystem
		no longer just MR and HDFS
		provides infrastructure for distributed computing and large scale data processing
	YARN
		enabler for new process models 
		cluster resource manager system
		allows any distributed program to run data in a hadoop cluster

comparison to relational databases
	disk drive trends
		seek time improving slower vs transfer rate
		seek time
			moving the disk's head to a particular place on disk to read or write data
			latency of disk operation
		transfer rate
			disk's bandwidth = amount of data that can be transmitted in a fix amount of time

	relational database
		update small data set
			traditional B-tree (data structure used for relational DB) works well
		update large data set
			seek data access pattern takes longer to read/write vs streaming 

		database normalization
			retain data integrity
				data doesn't change
			remove redundancy 
				set up tables to for intelligent joins

		good fit for
			point queries or updates 
			database has been indexed to deliver low latency retrieval and update times
			work on relatively small amount of data
			data is continually updated
			working with structured data (with a schema)

	hadoop 
		update large data set
			stream data access pattern = operates on transfer rate = faster read/write vs RDB
			B-Tree less efficient vs MR uses Sort/Merge to rebuild the database

		does not require data normalization
			hadoop does not want to read in nonlocal records in order to perform normalization
			assumes all data needed to create the bigger picture is contained in 1 place

		scales linerally with the size of data
			data is partitioned 
			map and reduce can work in parallel on separate partitions

			original input + cluster size = original speed
			double size of input > job will run 2x as slowly
			but if you double the number of disks > job will run as original speed 

		good fit for
			need to analyze whole dataset in batch fashion
			data is written once and read many times
			working with semi or unstructured data
				designed to interpret data at process time = schema-on-read


###########################
Ch 2 - MapReduce
###########################

data format
	easier and more efficient to process a smaller number of relatively large files

analyzing data using unix tools
	to speed up processing > run program in parallel 
	limitations to manual setup
		dividing work into equal pieces isn't always easy nor obvious
		file sizes differ > some processses finish earlier than others 
		more work = split input into file size chunks and assign each chunk to a process
		combining the results from independent processes requires more work
		limited by processing capacity on a single machine

analyzing data using hadoop
	map and reduce phases requires key - value pair input

	raw input
	0067011990999991950051507004...9999999N9+00001+99999999999... 
	0043011990999991950051512004...9999999N9+00221+99999999999... 
	0043011990999991950051518004...9999999N9-00111+99999999999... 
	0043012650999991949032412004...0500001N9+01111+99999999999...
	0043012650999991949032418004...0500001N9+00781+99999999999...

	mapper input with default key = offset of the beginning of the line from the beginning of the file
	(0, 0067011990999991950051507004...9999999N9+00001+99999999999...)
	(106, 0043011990999991950051512004...9999999N9+00221+99999999999...)
	(212, 0043011990999991950051518004...9999999N9-00111+99999999999...)
	(318, 0043012650999991949032412004...0500001N9+01111+99999999999...)
	(424, 0043012650999991949032418004...0500001N9+00781+99999999999...)

	map = data prep phase
		drop bad records
		pull out year and air temp

	mapper output with key as year and value interpretted as an int
	(1950, 0)
	(1950, 22)
	(1950, −11)
	(1949, 111)
	(1949, 78)

	map output > sort and group by key > reduce input

	reduce input
	(1949, [111, 78])
	(1950, [0, 22, −11])

	reduce function = iterate through the list and pick up the maximum reading

	reduce output
	(1949, 111)
	(1950, 22)

java MR
	Hadoop provides basic types = optimized for network serialization 
		hadoop LongWritable = java Long
		hadoop Text = java String
		hadoop IntWritable = java Integer
		hadoop Context
			allows the Mapper/Reducer to interact with the rest of the Hadoop system

	Job object
		forms the specification of the job
		gives you control over how the job is run

	running on cluster
		package code into JAR file 
		Hadoop will distribute around the cluster
		setJarByClass() will locate the relevant jar based on class name

	addInputPath()
		can have multiple input paths

	setOutputPath()
		single output path
		if output path already exists, Hadoop will fail to avoid overwrite

	$ export HADOOP_CLASSPATH=~/cbohara/hadoop-book/ch02-mr-intro/target/ch02-mr-intro-4.0.jar
		add application classes to the classpath
		hadoop script picks up

	$ hadoop MaxTemperature path/to/input/ output/
		when hadoop command invoked followed by classname as first arg
			launches JVM to run the class
		adds hadoop libraries and dependencies to the classpath
		picks up hadoop config

scaling out using HDFS + YARN
	allows hadoop to move the MapReduce computation to each machine hosting a part of the data
	orchestrated by YARN resource management system

data flow
	MR job
		unit of work the client wants to be performed 
		consists of
			input data
			MR program
			config info

	tasks
		hadoop performs MR job by dividing into tasks
			map tasks
			reduce tasks
		tasks are scheduled using YARN
		run on nodes in the cluster
		if a task fails > auto reschedule on a diff node

	splits
		hadoop divides the input to a MR job into fixed size pieces = input splits
		hadoop creates 1 map task per each split
		runs user defined map function for each record in the split
		good split size = size of the HDFS data block
			when you store a file in HDFS the system breaks the file down into a set of blocks

	data locality optimization
		hadoop does its best to run the map task on a node where the input data resides in HDFS

	map tasks 
		write output to local disk not HDFS
		intermediate output is processed by reduce tasks
		storing in HDFS with redudancy would be overkill
		if the node running the map task failed before map output has been consumed by reduce task > rerun map on another node

	reduce tasks 
		sorted map outputs transferred across the network to node where reduce task is running
		map output is merged > passed to user defined reduce function
		output is usually stored into HDFS

		number of reduce tasks is not governed by the size of the input
		needs to be specified

	shuffle
		data flow between map and reduce tasks
		when there are multiple reducers
			map task partitions their output
			create 1 partition per reduce task
			can be many keys with assoc value per partition

	combiner function
		can add combiner to minimize data transfer between map and reduce tasks 
		map output > combiner function > reduce input
		combiner function = same code as reducer function
			job.setMapperClass(MaxTemperatureMapper.class); 
			job.setCombinerClass(MaxTemperatureReducer.class); 
			job.setReducerClass(MaxTemperatureReducer.class);
		still need reducer to combine input from different mappers

	mapper 1 output		combiner 1 output
	(1950, 0)			(1950, 20)
	(1950, 20)
	(1950, 10)								reducer input		reducer output
											(1950, [20. 25])	(1950, 25)
	mapper 2 output		combiner 2 output
	(1950, 25)			(1950, 25)
	(1950, 15)

hadoop streaming
	write map and reduce functions in other languages besides java
	uses unix stdin and stdout
	ideal for text processing

	map input
		data passed via stdin > map function
	map output
		key-value written as single tab delimited line to stdout
	reduce input
		same as map output passed via stdin
		sorted by key
	reduce function
		writes results to stdout

$ cat input/ncdc/sample.txt | \ ch02-mr-intro/src/main/python/max_temperature_map.py | \
sort | ch02-mr-intro/src/main/python/max_temperature_reduce.py


#####################################
Ch 3 - HDFS
#####################################

distributed file system
	manage storage across multiple machines 
	network based
	optimized to work with very large files
	streaming data access = write once read many times = most efficient data processing pattern
		data set copied from source into HDFS
			slow to write first record
		various analysis on data is performed over time
			fast to read entire dataset


#####################################
Ch 5 - Hadoop I/O
#####################################

serialization
	turning structured objects into byte streams for
		transmission over a network
		writing to persistant storage

deserialization
	turning byte stream back into structured objects 

remote procedure calls (RPC)
	interprocess communication between the nodes implemented using RPC
	use serialization to render the message to binary system > remote node 
	remote node deserializes binary stream to original message

Writables = Hadoop's own serialization format
	defines 2 methods
		write
			writes its state to DataOut put binary stream
		readFields 
			reading its state from DataInput binary stream

WritableComparables 
	need to sort keys in sorting phase 
		makes comparison of types critical
	compare records read from a stream without deserializing them into objects
	avoids any overhead of object creation


#####################################
Ch 6 - Developing a MR Java App 
#####################################

Configuration API
	Configuration class 
		config properties and their values
		read from XML files
			type info not stored in XML
			properties interpreted as a given type when read
			get() allows you to specify default value
	core-default.xml
		default properties for the system
	properties
		those added later override earlier definitions
		specify final = cannot be overwritten
		override properties via command line using -Dproperty=value
	BE AWARE
		some properties have no effect when set in job submission
		ex: if you try to set yarn.nodemanager.resource.memory-mb in job submission it will be ignored
		needs to be sent in yarn-site.xml
	use -D to set property via command line
		ex: -D mapreduce.job.reduces=n

Writing Unit Test with MRUnit
	MRUnit
		testing library
		makes it easy to pass in known inputs to a mapper or reducer and check outputs
	Mapper
		use MapDriver
		if expected output values are not emitted by mapper, MR will fail test
		mapper ignores input key

Running on a cluster
	packaging a job
		local job runner uses single JVM to run a job so all you need to do is set the HADOOP_CLASSPATH
		need to specify job jar
			default
				auto search for the JAR on the driver’s classpath
				if it contains the class set in the setJarByClass() 
				dependent jar files can be in lib subdir within project
			explicit
				set JAR file by its file path using setJar()

	client classpath
		user’s client-side classpath set by hadoop jar <jar> is made up of
			job JAR file
			any JAR files in the lib directory of the job JAR file, and the classes directory (if present)
			classpath defined by HADOOP_CLASSPATH, if set
				if you are running using the local job runner without a job JAR
				have to set HADOOP_CLASSPATH to point to dependent classes and libraries

	task classpath
		on a cluster map and reduce tasks run in separate JVMs 
		classpaths NOT controlled by HADOOP_CLASSPATH
		user’s task classpath is made up of 
			job JAR file
			any JAR files contained in the lib directory of the job JAR file, and the classes directory (if present)
			any files added to the distributed cache using the -libjars option
			OR
			addFileToClassPath() in Job()

	packaging dependencies
		best practice
			keep the libraries separate from the job JAR 
			add libraries to the client classpath via HADOOP_CLASSPATH
			add libraries to the task classpath via -libjars
		using the distributed cache
			dependencies don’t need rebundling in the job JAR
			fewer transfers of JAR files around the cluster
			files may be cached on a node between tasks

Job, Task, and Task Attempt IDs
	MapReduce job IDs are generated from YARN application IDs
		use epoch timestamp
	tasks belong to a job 
		task_1410450250506_0003_m_000003
			4th map task of the job
	tasks fail > multiple executions of same task > identified by unique attempt ID
		attempt_1410450250506_0003_m_000003_0
			first attempt of running map task

Job history
	refers to the events and configuration for a completed MapReduce job
	retained regardless of whether the job was successful
	history log includes job, task, and attempt events

Debugging a job
	with programs running on many nodes how do we find and examine the output of debug statements?
	log to stderr
	create custom counter

Hadoop logs
	System daemon logs
	HDFS audit logs
	MR job history logs
		overall job summary
	MR task logs
		syslog
			each task child process produces a log file using log4j
		stdout
			file for data sent to stdout
			WARNING = in streaming stdoutput is used for map or reduce output so won't show up in log
		stderr
			file for stderr
		written in userlogs subdir of YARN_LOG_DIR

	YARN log aggregation
		yarn.log-aggregation-enable=true
		allows you to view logs via web UI or
		using mapred job -logs

	using Apache commons logging API
		default log level is INFO so need to set to debug if want to see in logs
		in cluster
			-D mapreduce.map.log.level=DEBUG
		local
			HADOOP_ROOT_LOGGER=DEBUG,console

