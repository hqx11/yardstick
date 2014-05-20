# Yardstick
Yardstick is a framework for writing benchmarks. Specifically it helps with writing benchmarks for clustered or otherwise distributed systems.

The framework comes with a default set of probes that collect various metrics during benchmark execution. Probes can be turned on or off in configuration. You can use a probe  for measuring throughput and latency, or a probe that gathers `vmstat` statistics, etc... At the end of benchmark execution, Yardstick automatically produces files with probe points.

See [Yardstick GridGain](https://github.com/gridgain/yardstick-gridgain) as an example of Yardstick framework usage.

## Creating Yardstick Benchmarks
There are two main interfaces that need to be implemented, `BenchmarkServer` and `BenchmarkDriver`: 
* `BenchmarkDriver` is an instance of the benchmark that performs some operation that needs to be tested. 
* `BenchmarkServer` is the remote server that the BenchmarkDriver communicates with.

You can benchmark any distributed operation with Yardstick. For example, if you want to measure message processing time in your application, then you can put message sending logic into `BenchmarkDriver`, and message processing logic to one or more remote `BenchmarkServers`.

It is as simple as this. Yardstick will measure throughput, latency, and other metrics for you automatically and produce nice graphs at the end.

## Running Yardstick Benchmarks
The easiest way to run benchmarks is by executing `bin/benchmark-run-all.sh` script which will automatically start benchmark driver and remote servers base based on the properties file passed in (`config/benchmark.properties` used by default):

	$ bin/benchmark-run-all.sh config/benchmark.properties
	
This script will automatically restart benchmark driver and remote servers for every benchmark configuration provided in `config/benchmark.properties` file.

At the end of the run, you can generate graphs by executing `bin/jfreechart-graph-gen.sh` script with folders that contain benchmark results.

	$ bin/jfreechart-graph-gen.sh -i results_2014-05-16_00-28-01,results_2014-05-15_18-38-14


### Starting Remote Servers
If you do not wish to run `bin/benchmark-run-all.sh` script and prefer to have more control over starting and stopping remote servers, you can use `benchmark-servers-start.sh` script directly.

	$ bin/benchmark-servers-start.sh config/benchmark.properties

**Remote Server Log Files** are stored in the `logs` folder.

### Starting Benchmark Driver
Again, if you do not wish to run `bin/benchmark-run-all.sh` script, you can start benchmark driver directly by executing `benchmark-run.sh` script.

	$ bin/benchmark-run.sh config/benchmark.properties

### Stopping Remote Servers
To stop remote servers after the benchmark is finished, you can execute `benchmark-servers-stop.sh` script.

	$ bin/benchmark-servers-stop.sh config/benchmark.properties

### Properties And Command Line Arguments

The following properties can be defined in benchmark properties file:

* `BENCHMARK_DEFAULT_PROBES` - list of default probes
* `BENCHMARK_PACKAGES` - packages where the specified benchmark is searched by reflection mechanism
* `BENCHMARK_WRITER` - probe point writer class name (by default CSV writer is used)
* `HOSTS` - comma-separated list of IP addresses where servers should be started, one server per host
* `REMOTE_USER` - SSH user for logging in to remote hosts
* `CONFIGS` - comma-separated list of benchmark run configurations which are passed to the servers and to the benchmarks

Example of `benchmark.properties` file

	# List of default probes.
	BENCHMARK_DEFAULT_PROBES=ThroughputLatencyProbe

	# Packages where the specified benchmark is searched by reflection mechanism.
	BENCHMARK_PACKAGES=org.yardstick

	# Probe point writer class name.
	# BENCHMARK_WRITER=

	# Comma-separated list of remote hosts to run BenchmarkServers on.
	HOSTS=localhost,localhost
	
	# Remote username
	# REMOTE_USER=

	# Comma-separated list of benchmark driver configuration parameters.
	CONFIGS="\
	--localBind localhost --duration 30 -t 2 -sn EchoServer -dn EchoServerBenchmark,\
	--localBind localhost --duration 30 -t 4 -sn EchoServer -dn EchoServerBenchmark\
	"


The following properties can be defined in the benchmark configuration:

* `-cfg <path>` or `--config <path>` - framework configuration file path
* `-dn <name>` or `--driverName <name>` - driver name (required for the driver)
* `-sn <name>` or `--serverName <name>` - server name (required for the server)
* `-p <list>` or `--packages <list>` - comma separated list of packages for benchmarks
* `-pr <list>` or `--probes <list>` - comma separated list of probes for benchmarks
* `-wr <name>` or `--writer <name>` - probe point writer class name
* `-t <num>` or `--threads <num>` - thread count (set to 'cpus * 2')
* `-d <time>` or `--duration <time>` - test duration, in seconds
* `-w <time>` or `--warmup <time>` - warmup time, in seconds
* `-sh` or `--shutdown` - flag indicating whether to invoke shutdown hook or not
* `-of <path>` or `--outputFolder <path>` - output folder for benchmark results, current folder is used by default

For example if we need to run EchoServer server on localhost and EchoServerBenchmark benchmark on localhost, 
the test should be 20 seconds then the following configuration should be specified in run properties file:

* `HOSTS=localhost`
* `CONFIGS="--duration 20 -sn EchoServer -dn EchoServerBenchmark"`

## JFreeChart graphs
Yardstick goes with the script `jfreechart-graph-gen.sh` that builds JFreeChart graphs using probe points.

`jfreechart-graph-gen.sh` script accepts the following arguments:

* `-i <list>` or `--inputFolders <list>` - space separated list of input folders which contains folders 
with probe results files (required)
* `-cc <num>` or `--chartColumns <num>` - number of columns that the charts are displayed in on the resulted page
* `-gm <mode>` or `--generationMode <mode>` - mode that defines the way how different benchmark runs are compared 
with each other

Generation modes:

* `STANDARD` - all benchmark results are displayed on separate graphs

    Example: `bin/jfreechart-graph-gen.sh -i results_2014-05-20_03-19-21 results_2014-05-20_03-20-35 -gm STANDARD`

    Output: images with graphs and Results.html file are generated for every benchmark run from the folders above, 
the generated files are located in the benchmark run folder, near probe results files 


* `COMPARISON` - benchmarks from multiple folders (space separated) are paired together

    Example: `bin/jfreechart-graph-gen.sh -i results_2014-05-20_03-19-21 results_2014-05-20_03-20-35 -gm COMPARISON`

    Output: folder `results_comparison_2014-05-20_03-19-21_2014-05-20_03-20-35` is created, it contains the list of folders
with images and Results.html file. In this mode the first benchmark run from the first input folder is compared with
the first benchmark run from the second input folder, the second run with the second run and etc. 
 


* `COMPOUND` - benchmarks from multiple folders (space separated) are shown together, it's a default mode

    Example: `bin/jfreechart-graph-gen.sh -i results_2014-05-20_03-19-21 results_2014-05-20_03-20-35 -gm COMPOUND`

    Output: folder `results_compound_2014-05-20_03-19-21_2014-05-20_03-20-35` is created, it contains images with graphs 
and Results.html. All benchmarks results will be displayed on one graph

## Maven Install
The easiest way to get started with Yardstick in your project is to use Maven dependency management:

```xml
<dependency>
    <groupId>org.yardstick</groupId>
    <artifactId>yardstick</artifactId>
    <version>${yardstick.version}</version>
</dependency>
```

You can copy and paste this snippet into your Maven POM file. Make sure to replace version with the one you need.

Yardstick is shipped with scripts that run servers and drivers, these scripts can be used for your benchmarks.
In order to have them just unzip `yardstick-resources.zip` maven artifact.
Also this can be done by copying and pasting the following code snippet to the POM file:

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-dependency-plugin</artifactId>
            <executions>
                <execution>
                    <id>unpack</id>
                    <phase>package</phase>
                    <goals>
                        <goal>unpack</goal>
                    </goals>
                    <configuration>
                        <artifactItems>
                            <artifactItem>
                                <groupId>org.yardstick</groupId>
                                <artifactId>yardstick</artifactId>
                                <version>${yardstick.version}</version>
                                <type>zip</type>
                                <classifier>resources</classifier>
                                <outputDirectory>${basedir}</outputDirectory>
                            </artifactItem>
                        </artifactItems>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

The scripts will be unpacked to `bin` folder by command `mvn package`. See how it's done in 
[Yardstick GridGain](https://github.com/gridgain/yardstick-gridgain).

## Issues
Use GitHub [issues](https://github.com/gridgain/yardstick/issues) to file bugs.

## License
Yardstick is available under [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) license.
