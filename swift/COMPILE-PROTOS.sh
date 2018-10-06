#!/bin/sh
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

protoc ../protos/kiosk.proto \
  ../protos/api-common-protos/google/type/latlng.proto  \
  -I ../protos/api-common-protos \
  -I ../protos \
  --include_imports \
  --include_source_info \
  --descriptor_set_out=./kiosk_descriptor.pb \
  --swift_out=Sources \
  --swiftgrpc_out=Sources

# move Swift files to the Sources directory
#find googleapis -name "*.swift" -exec mv {} Sources \;

