# S3HyperSync

S3HyperSync is a high-performance, memory-efficient, and cost-effective tool designed for synchronizing files between
S3-compatible storage services. This tool is optimized for speed, reliability, and
minimizing AWS costs, making it an ideal solution for large-scale data synchronization or backup tasks.

## Features

- Concurrent Workers: Up to 64 concurrent workers for parallel uploads.
- UUID Booster: Significantly increases indexing speed when source prefixes contain UUIDs.
- Custom S3 Endpoints: Allows configuring custom endpoints for lower-cost S3-compatible storage.
- Avoids GET Requests: Minimizes the use of GET requests to reduce costs.
- Cost-Effective Storage: Automatically selects the most appropriate and cost-effective S3 storage tiers.
- Minimized PUT Requests: Attempts to reduce the number of multipart uploads to save on request costs.
- Custom Endpoints: Supports other S3-compatible storage services.
- Stream-Based Approach: Processes files without holding all items in memory, allowing for efficient memory usage even
  with large datasets.

## Performance
In performance testing on AWS Fargate, the iteration speed was between 8000 files per second and 100.000 files per second using the
UUID booster feature.
Copy speed was either limited by the network interface or around 500 files per second for smaller files.

## Usage

Get the jar file and a working JVM. Then run the command on your CLI:

> java -jar S3HyperSync.jar \
> --sk <source-s3-key> \
> --ss <source-s3-secret> \
> --sr <source-s3-region> \
> --sb <source-s3-bucket> \
> --sx <source-s3-prefix> \
> --tk <target-r2-key> \
> --ts <target-r2-secret> \
> --tr <target-r2-region> \
> --tb <target-r2-bucket> \
> --tx <target-r2-prefix> \
> --te https://<custom-r2-endpoint> \
> --tp \
> --workers 32 \
> --putCutoffSize 104857600 \
> --multiPartSize 104857600 \
> --sync IF_SIZE_CHANGED \
> --tier INTELLIGENT_TIERING \
> --dryrun

## Contributing

We welcome contributions from the community. If you find a bug or have a feature request, please open an issue on
GitHub. If you want to contribute code, please fork the repository and submit a pull request.

## License

S3HyperSync is released under the MIT License. See the LICENSE file for more details.

## Acknowledgements

We would like to thank all the contributors and the open-source community for their support and contributions to this
project.
