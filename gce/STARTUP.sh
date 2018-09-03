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

#
# This script runs as root as the startup script of a Google Container
# Engine instance. For more about startup scripts, see 
#	https://cloud.google.com/compute/docs/startupscript
#

# make sure that the HOME environment variable is set.
export HOME=/root
echo "home is $HOME"

apt-get update
apt-get install unzip -y

mkdir -p /root/downloads

# Download and install go.
cd /root/downloads
wget https://dl.google.com/go/go1.11.linux-amd64.tar.gz
tar xzf go1.11.linux-amd64.tar.gz -C /usr/local

# Download and install protoc.
cd /root/downloads
wget https://github.com/protocolbuffers/protobuf/releases/download/v3.6.1/protoc-3.6.1-linux-x86_64.zip
mkdir -p /usr/local/protobuf
unzip -d /usr/local/protobuf protoc-3.6.1-linux-x86_64.zip

# Setup user environment.
echo "export GOPATH=\$HOME/go" >> /root/.profile
echo "export PATH=\$GOPATH/bin:/usr/local/go/bin:/usr/local/protobuf/bin:\$PATH" >> /root/.profile
source /root/.profile

# Download Kiosk API code.
mkdir -p /root/go/bin
go get -d github.com/googleapis/kiosk

# Generate Protocol Buffer and gRPC support files.
cd /root/go/src/github.com/googleapis/kiosk
sh COMPILE-PROTOS.sh

# Build kiosk binaries.
go get github.com/googleapis/kiosk/server
go get github.com/googleapis/kiosk/k

# Report on the results.
echo "Done!"
which server
which k

# Run the server
nohup server &
