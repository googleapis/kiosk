#!/bin/bash
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
  curl -L -O https://github.com/googleapis/api-common-protos/archive/master.zip
  unzip master.zip
  rm -f master.zip
  mv api-common-protos-master protos/api-common-protos
fi

go get github.com/golang/protobuf/protoc-gen-go
go get google.golang.org/grpc

go get github.com/googleapis/gapic-generator-go/cmd/protoc-gen-go_gapic
pushd $GOPATH/src/github.com/googleapis/gapic-generator-go
git checkout v0.4.0
go install github.com/googleapis/gapic-generator-go/cmd/protoc-gen-go_gapic
go install github.com/googleapis/gapic-generator-go/cmd/protoc-gen-go_cli
popd

mkdir -p generated
mkdir -p gapic
mkdir -p kctl

protoc protos/kiosk.proto \
  -I protos/api-common-protos \
  -I protos \
  --include_imports \
  --include_source_info \
  --descriptor_set_out=generated/kiosk_descriptor.pb \
  --go_out=plugins=grpc:$GOPATH/src \
  --go_gapic_out $GOPATH/src/ \
  --go_gapic_opt 'go-gapic-package=github.com/googleapis/kiosk/gapic;gapic' \
  --go_cli_out kctl/ \
  --go_cli_opt "gapic=github.com/googleapis/kiosk/gapic" \
  --go_cli_opt "root=kctl"

protoc endpoints/kiosk_with_http.proto \
  -I protos/api-common-protos \
  -I endpoints \
  --include_imports \
  --include_source_info \
  --descriptor_set_out=generated/kiosk_with_http_descriptor.pb
