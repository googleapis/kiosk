# The Kiosk API

This example demonstrates the use of Protocol Buffers to define and implement a
networked API. The Kiosk API allows users to manage a collection of displays
that are able to display digital signs containing text and images. The Kiosk
API includes a streaming method that allows displays to immediately update in
response to configuration changes on the API server. The Kiosk API is
implemented using gRPC, which includes support for streaming APIs. All
non-streaming API methods can also be provided as REST services using standard
gRPC to JSON transcoding.

## Goals

This project aims to demonstrate the following:

- How to design an API with Protocol Buffers
- How to run protoc and the Protocol Buffer and gRPC code generators.
- How to write a gRPC server in multiple languages (currently Go and Swift).
- How to build a gRPC client in multiple languages (currently Go and Swift).
- How to generate API clients in other languages.
- How to annotate the API for HTTP transcoding.
- How to manage the API with Google Cloud Endpoints.

## Copyright

Copyright 2018, Google LLC.

## License

Released under the Apache 2.0 license.

## Contributing

Please get involved! See our [guidelines for contributing](CONTRIBUTING.md).

## Testing

Use the `go test` command to verify a running server.

By default, this looks for an API server running at localhost on port 8080. Use
the KIOSK_SERVER and KIOSK_PORT environment variables to override this.

## Run on Google Compute Engine

The [gce](gce) directory contains a [SETUP.sh](gce/SETUP.sh) script that can be
used to start a kiosk server on
[Google Compute Engine](https://cloud.google.com/compute/).
