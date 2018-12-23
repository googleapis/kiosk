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
#
# Use this script to regenerate the Protocol Buffer and gRPC files
# needed to build the example.
#
# Note that it requires updated protoc, protoc-gen-swift, and
# protoc-gen-swiftgrpc binaries and assumes that protoc-gen-swift 
# is installed in $HOME/local/bin.

if [ ! -d "../protos/api-common-protos" ]; then
  curl -L -O https://github.com/googleapis/api-common-protos/archive/master.zip
  unzip master.zip
  rm -f master.zip
  mv api-common-protos-master ../protos/api-common-protos
fi

if [ ! -d "third_party/grpc-dart" ]; then
  mkdir -p third_party
  curl -L -O https://github.com/grpc/grpc-dart/archive/master.zip
  unzip master.zip
  rm -f master.zip
  mv grpc-dart-master third_party/grpc-dart
fi

mkdir -p lib/src/generated

protoc ../protos/kiosk.proto \
  ../protos/api-common-protos/google/type/latlng.proto  \
  -I ../protos/api-common-protos \
  -I ../protos \
  --dart_out=grpc:lib/src/generated

# copy standard protos into project
DST=lib/src/generated/google/protobuf
mkdir -p $DST
SRC=third_party/grpc-dart/example/googleapis/lib/src/generated/google/protobuf
cp $SRC/empty.*.dart $DST
cp $SRC/timestamp.*.dart $DST

