# Run the Kiosk API with Envoy Proxy

Using
[gRPC-JSON Transcoding](https://www.envoyproxy.io/docs/envoy/latest/configuration/http_filters/grpc_json_transcoder_filter)
the Envoy proxy can publish a JSON REST interface to a gRPC service.

This shows how to do that for the Kiosk API.

### Prerequisites

- You have the Kiosk server running.
- You have Envoy installed on the same system.

### Steps

1.  Rebuild the protos with API descriptors for Envoy to consume.

    ```bash
    ./COMPILE_PROTOS.sh
    ```

1.  Start Envoy.

    ```bash
    envoy -c envoy.yaml
    ```

1.  Access the API with curl or a web browser. For example
    [this address](http://localhost:8081/v1/kiosks) calls the ListKiosks API
    method.
