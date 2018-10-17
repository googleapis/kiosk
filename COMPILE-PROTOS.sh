#!/bin/sh
# Copyright 2018 Google LLC. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ ! -d "protos/api-common-protos" ]; then
  curl -L -O https://github.com/googleapis/api-common-protos/archive/input-contract.zip
  unzip input-contract.zip
  rm -f input-contract.zip
  mv api-common-protos-input-contract protos/api-common-protos
fi

go get github.com/googleapis/gapic-generator-go/cmd/protoc-gen-go_gapic
go get github.com/golang/protobuf/protoc-gen-go
go get google.golang.org/grpc

mkdir -p generated

protoc protos/kiosk.proto \
  --go_gapic_out $GOPATH/src/ \
  --go_gapic_opt 'github.com/googleapis/kiosk/kioskgapic;kioskgapic'  \
  -I protos/api-common-protos \
  -I protos \
  --include_imports \
  --include_source_info \
  --descriptor_set_out=generated/kiosk_descriptor.pb \
  --go_out=plugins=grpc:$GOPATH/src

protoc endpoints/kiosk_with_http.proto \
  -I protos/api-common-protos \
  -I endpoints \
  --include_imports \
  --include_source_info \
  --descriptor_set_out=generated/kiosk_with_http_descriptor.pb

# TODO(ndietz) remove this section when pongad fixes w/cloud-go team
# remove generated code specific to Google gapics in order to compile kioskgapic lib
sed -i '/	"cloud.google.com\/go\/internal\/version"/d' ./kioskgapic/display_client.go
sed -i '/	kv := append(\[]string{"gl-go", version.Go()}, keyval...)/d' ./kioskgapic/display_client.go
sed -i '/	kv = append(kv, "gapic", version.Repo, "gax", gax.Version, "grpc", grpc.Version)/d' ./kioskgapic/display_client.go
sed -i '/	c.xGoogMetadata = metadata.Pairs("x-goog-api-client", gax.XGoogHeader(kv...))/d' ./kioskgapic/display_client.go