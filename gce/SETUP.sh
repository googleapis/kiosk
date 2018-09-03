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

# This script uses the gcloud command to create and configure a 
# Google Compute Engine virtual machine to run the kiosk server.
# To run it, you'll need to first authenticate by running
#	`gcloud auth login`.
#
# Call this script with two arguments: your Google Cloud project name
# and a unique name for the new GCE instance that will be started.

PROJECT=$1
INSTANCE=$2

if [ -z "$PROJECT" ] || [ -z "$INSTANCE" ]; then
    echo "usage: $0 <project> <instance name>"
    exit
fi

gcloud compute --project=$PROJECT \
	instances create $INSTANCE \
	--zone=us-west1-a \
	--machine-type=g1-small \
	--subnet=default \
	--network-tier=PREMIUM \
    	--metadata startup-script-url=https://raw.githubusercontent.com/googleapis/kiosk/master/gce/STARTUP.sh \
	--maintenance-policy=MIGRATE \
	--scopes=https://www.googleapis.com/auth/cloud-platform \
	--tags=http-server \
	--image=ubuntu-1604-lts-drawfork-v20180810 \
	--image-project=eip-images \
	--boot-disk-size=200GB \
	--boot-disk-type=pd-standard
	
gcloud compute --project=$PROJECT \
	firewall-rules create default-allow-http \
	--direction=INGRESS \
	--priority=1000 \
	--network=default \
	--action=ALLOW \
	--rules=tcp:8080 \
	--source-ranges=0.0.0.0/0 \
	--target-tags=http-server

