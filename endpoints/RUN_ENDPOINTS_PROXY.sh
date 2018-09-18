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

PROJECT_ID=$1
API_NAME=$2
SERVICE_ACCOUNT_KEY=$3

if [ -z "$PROJECT_ID" ] || [ -z "$API_NAME" ] || [ -z "$SERVICE_ACCOUNT_KEY" ]; then
    echo "usage: $0 <project ID> <api name> <service account keyfile>"
    exit
fi

set -e

SERVICE_ACCOUNT_MOUNT_DIR=$(dirname "${SERVICE_ACCOUNT_KEY}")
SERVICE_ACCOUNT_FILENAME=$(basename "${SERVICE_ACCOUNT_KEY}")
SERVICE_NAME="${API_NAME}.endpoints.${PROJECT_ID}.cloud.goog"

LOCAL_HOSTNAME="localhost"
if [ "$(uname)" == "Darwin" ]; then
  LOCAL_HOSTNAME="docker.for.mac.localhost"
fi

echo "Starting the Endpoints proxy via running Docker as root..."
docker_ps=$(sudo docker run \
    --detach \
    --publish=8082:8082 \
    --publish=8083:8083 \
    --volume="${SERVICE_ACCOUNT_MOUNT_DIR}:/esp" \
    gcr.io/endpoints-release/endpoints-runtime:1 \
    --service="${SERVICE_NAME}" \
    --rollout_strategy=managed \
    --http_port=8082 \
    --http2_port=8083 \
    --backend=grpc://${LOCAL_HOSTNAME}:8080 \
    --service_account_key="/esp/${SERVICE_ACCOUNT_FILENAME}")

echo "Running Endpoints proxy with HTTP port 8082, gRPC port 8083."
echo ""
echo "For logs, run:"
echo "    docker logs ${docker_ps}"
echo ""
echo "To point the kiosk client at this proxy, run:"
echo "    export KIOSK_PORT=8083"
echo "    export KIOSK_APIKEY=<your API key>"
