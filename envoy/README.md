# Run the Kiosk API with Google Cloud Endpoints

## On your local machine

### Prerequisites

*   You have the Kiosk server running.
*   You have a envoy installed on the same system.

### Steps

All steps should be run from the root directory of this repository.

1.  Rebuild the protos with API descriptors for envoy to consume.

    ```bash
    ./COMPILE_PROTOS.sh
    ```

1.  Start the envoy proxy.

    ```bash
    envoy -c envoy.yaml
    ```

