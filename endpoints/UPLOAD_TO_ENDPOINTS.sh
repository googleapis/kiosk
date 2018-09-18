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
KIOSK_PROTO=$3

if [ -z "$PROJECT_ID" ] || [ -z "$API_NAME" ] || [ -z "$KIOSK_PROTO" ]; then
    echo "usage: $0 <project ID> <api name> kiosk|kiosk_with_http"
    exit
fi

set -e

DESCRIPTOR_FILE="generated/${KIOSK_PROTO}_descriptor.pb"

if [ ! -f "${DESCRIPTOR_FILE}" ]; then
    echo "Could not find ${DESCRIPTOR_FILE}. Make sure you're in the root " \
         "directory and have run COMPILE_PROTOS.sh."
    exit
fi

cat endpoints/service.yaml.template \
    | sed s/\$\{PROJECT_ID\}/${PROJECT_ID}/ \
    | sed s/\$\{API_NAME\}/${API_NAME}/ \
    > generated/service.yaml

echo "Uploading to Endpoints..."
gcloud endpoints services deploy generated/service.yaml "${DESCRIPTOR_FILE}"
echo "Done!"
echo ""
echo "To start the Endpoints proxy locally, run endpoints/RUN_ENDPOINTS_PROXY.sh".
