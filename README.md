# The Kiosk API

This example demonstrates the use of Protocol Buffers to define and implement a
networked API. The Kiosk API allows users to manage a collection of displays
that are able to display digital signs containing text and images. The Kiosk API
includes a streaming method that allows displays to immediately update in
response to configuration changes on the API server. The Kiosk API is
implemented using gRPC, which includes support for streaming APIs. All
non-streaming API methods can also be provided as REST services using standard
gRPC to JSON transcoding.

## Goals

This project aims to demonstrate the following:

- How to design an API with Protocol Buffers
- How to run protoc and the Go Protocol Buffer and gRPC code generators.
- How to write a gRPC server with Go.
- How to build a gRPC client with Go.
- How to generate API clients in other languages.
- How to annotate the API for HTTP transcoding.
- How to run the API behind Envoy.
- How to manage the API with Google Cloud Endpoints.

## Copyright

Copyright 2018, Google LLC.

## License

Released under the Apache 2.0 license.

## Contributing

Please get involved! See our [guidelines for contributing](CONTRIBUTING.md).
