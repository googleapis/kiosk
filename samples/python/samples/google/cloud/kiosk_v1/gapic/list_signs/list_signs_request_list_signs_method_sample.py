# -*- coding: utf-8 -*-
#
# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# DO NOT EDIT! This is a generated sample ("Request",  "list_signs_method_sample")

# To install the latest published package dependency, execute the following:
#   pip install google-cloud-kiosk

import sys

# [START sample]

from google.cloud import kiosk_v1


def sample_list_signs():
    """List Signs Method Sample"""

    # [START sample_core]

    client = kiosk_v1.DisplayClient()

    response = client.list_signs()
    for sign in response.signs:
        print('Sign: {}'.format(sign))

    # [END sample_core]


# [END sample]


def main():
    # FIXME: Convert argv from strings to the correct types.
    sample_list_signs(*sys.argv[1:])


if __name__ == '__main__':
    main()
