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

# Opens the firewall to allow http connections on port 8080.
# To run it, you'll need to first authenticate by running
#	`gcloud auth login`.
#
# Call this script with your Google Cloud project name.

PROJECT=$1

if [ -z "$PROJECT" ]; then
    echo "usage: $0 <project>"
    exit
fi

gcloud compute --project=$PROJECT \
	firewall-rules create default-allow-http \
	--direction=INGRESS \
	--priority=1000 \
	--network=default \
	--action=ALLOW \
	--rules=tcp:8080 \
	--source-ranges=0.0.0.0/0 \
	--target-tags=http-server

