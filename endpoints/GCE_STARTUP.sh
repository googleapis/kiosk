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
# This script runs as root as the startup script of a Google Compute
# Engine instance. For more about startup scripts, see 
# https://cloud.google.com/compute/docs/startupscript
#

# Run startup scripts from gce/ directory to start the kiosk server.
curl https://raw.githubusercontent.com/googleapis/kiosk/master/gce/STARTUP.sh | bash

# Install Docker. Steps adapted from
# https://docs.docker.com/install/linux/docker-ce/ubuntu/
apt-get install -y \
     apt-transport-https ca-certificates curl gnupg2 software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"
apt-get update
apt-get install -y docker-ce

# Get project ID from compute metadata server
PROJECT_ID=$(
    curl "http://metadata.google.internal/computeMetadata/v1/project/project-id" \
        -H "Metadata-Flavor: Google")
SERVICE_NAME="kiosk.endpoints.${PROJECT_ID}.cloud.goog"

docker run \
    --detach \
    --name=esp \
    --net=host \
    gcr.io/endpoints-release/endpoints-runtime:1 \
    --service="${SERVICE_NAME}" \
    --rollout_strategy=managed \
    --http_port=8082 \
    --http2_port=8083 \
    --backend=grpc://localhost:8080

echo "Started Endpoints proxy, HTTP port 8082, gRPC port 8083"
