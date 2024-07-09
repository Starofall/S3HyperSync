# S3HyperSync
S3HyperSync is a high-performance, memory-efficient, and cost-effective tool for synchronizing files between S3-compatible storage services. Optimized for speed, reliability, and minimizing AWS costs, it's ideal for large-scale data synchronization and backup tasks. Utilizing Pekko, it adopts a stream-only approach to maintain low memory requirements.

## Origin
Developed for creating daily backups of huge S3 buckets with millions of files and terabyte of data to an seperate AWS account.

## Cost Effective
To sync large S3 buckets, S3HyperSync compares directories using two iterator streams for the source and target, reducing the need for costly GetObject requests, especially for DEEP_ARCHIVE storage. 
It minimizes expensive MultiPart uploads, as they count as multiple PutObject calls.

## Performance
Performance tests on AWS Fargate show iteration speeds between 8,000 to 100,000 files per second with the UUID booster feature. 
Copy speeds reach around 600MB/s on a c6gn.4xlarge or 800 files per second for smaller files.

## UUID Booster
The UUID booster feature can be used if data is suffixed with a uuid. E.g. s3://bucket/videos/$UUID
In this case the tool creates 16 iterators and processes them in parallel for extremly fast bucket comparison.

## Installation

Download the JAR file from the Release Section or build it yourself with sbt assembly.

## Usage

```
S3HyperSync.jar 0.1.5
Usage: java -jar S3HyperSync.jar [OPTIONS]
A fast, efficient, cost-reducing, and memory-efficient S3 sync tool.
Options:
      --dry-run                  Show what would be copied without actually
                                 copying
      --multipart-size  <arg>    Size of each part in a multipart upload (in
                                 bytes)
      --no-color                 Disable colored output
      --put-cutoff-size  <arg>   Files larger than this size (in bytes) are
                                 uploaded using multipart
      --source-bucket  <arg>     Source S3 Bucket
      --source-endpoint  <arg>   Source S3 Endpoint
      --source-key  <arg>        Source S3 Key
      --source-path-style        Use path style for source S3
      --source-prefix  <arg>     Source S3 Prefix (must end with /)
      --source-region  <arg>     Source S3 Region
      --source-secret  <arg>     Source S3 Secret
      --storage-tier  <arg>      Storage tier: STANDARD, INTELLIGENT_TIERING,
                                 GLACIER_IR, GLACIER_IR_AUTO, DEEP_ARCHIVE,
                                 DEEP_ARCHIVE_AUTO
      --sync  <arg>              Sync mode: ALWAYS, MISSING, CHANGED
      --target-bucket  <arg>     Target S3 Bucket
      --target-endpoint  <arg>   Target S3 Endpoint
      --target-key  <arg>        Target S3 Key
      --target-path-style        Use path style for target S3
      --target-prefix  <arg>     Target S3 Prefix (must end with /)
      --target-region  <arg>     Target S3 Region
      --target-secret  <arg>     Target S3 Secret
      --timeout  <arg>           Kills the process after N seconds
      --uuid-boost               Increase index speed if source prefix contains
                                 UUIDs
  -v, --verbose                  Verbose level (use multiple -v for increased
                                 verbosity)
      --workers  <arg>           Number of workers
  -h, --help                     Show help message
      --version                  Show version of this program
```
## Contributing

We welcome contributions from the community. If you find a bug or have a feature request, please open an issue on
GitHub. If you want to contribute code, please fork the repository and submit a pull request.

## License

S3HyperSync is released under the MIT License. See the LICENSE file for more details.

## Acknowledgements

We would like to thank all the contributors and the open-source community for their support and contributions to this
project.
