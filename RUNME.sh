#!/bin/sh

if [ ! -d "protos/api-common-protos" ]; then
  curl -L -O https://github.com/googleapis/api-common-protos/archive/master.zip
  unzip master.zip
  rm -f master.zip
  mv api-common-protos-master protos/api-common-protos
fi

go get google.golang.org/grpc

mkdir -p generated

protoc protos/kiosk.proto -I protos/api-common-protos -I protos --go_out=plugins=grpc:generated

