# S3HyperSync

S3HyperSync is a high-performance, memory-efficient, and cost-effective tool designed for synchronizing files between
S3-compatible storage services. This tool is optimized for speed, reliability, and
minimizing AWS costs, making it an ideal solution for large-scale data synchronization or backup tasks.

It uses Pekko under the hood to have a stream-only approach that keeps memory requirements low.



## Cost Effective
To sync big S3 buckets to e.g. a backup bucket, the tool needs to compare both directories.
For this, some tools use ListBucket for the source, but GetObject for the target bucket.
If your data is stored as DEEP_ARCHIVE, these GetObject requests get expensive fast.
S3HyperSync uses two iterator streams for the source and target stream and thereby only creates ListBucket
requests if nothing needs to be synced.

Another issue if your store data e.g. in the DEEP_ARCHIVE tier is that the amount of PutObject requests
can get expensive. S3HyperSync therefore tries to minimize the use of MultiPart uploads, as they
are charged with at least three PutObject requests.

## Performance
In performance testing on AWS Fargate, the iteration speed was between 8.000 files per second and 100.000 files per second using the
UUID booster feature (which creates listDirectory iterators for each possible first uuid letter in lowercase).
Copy speed is around 600MB/s on a c6gn.8xlarge or around 500 files per second for smaller files.

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
