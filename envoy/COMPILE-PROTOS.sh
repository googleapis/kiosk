#!/bin/bash
#
# This script compiles the kiosk protos into a file descriptor set
# that can be read by envoy to configure gRPC-JSON transcoding.
# https://www.envoyproxy.io/docs/envoy/latest/configuration/http_filters/grpc_json_transcoder_filter

echo "Compiling kiosk API protos into proto.pb."
protoc -I../protos -I../protos/api-common-protos --include_imports --include_source_info \
  --descriptor_set_out=proto.pb \
  kiosk.proto

echo "To demonstrate gRPC-JSON transcoding, run 'envoy -c envoy.yaml' in this directory."
echo "Envoy will proxy requests to a kiosk server running on port 8080."
echo "The HTTP server is running on port 8081."
