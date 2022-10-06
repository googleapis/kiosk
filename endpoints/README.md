# Run the Kiosk API with Google Cloud Endpoints

## On your local machine

### Prerequisites

- You have the Kiosk server running.
- [Docker is installed](https://docs.docker.com/engine/installation/).
- You have a GCP project to use for the following steps.
- You have a
  [service account](https://cloud.google.com/endpoints/docs/grpc/running-esp-localdev#create_service_account)
  that the local API proxy can act as, with a local service account key file.
  This service account should be from the same GCP project above.

### Steps

All steps should be run from the root directory of this repository.

1.  Rebuild the protos with API descriptors for Endpoints to consume. These
    files will be output to the `generated/` directory.

    ```bash
    ./COMPILE_PROTOS.sh
    ```

1.  Upload your API to Endpoints, into your GCP project.

    ```bash
    ./endpoints/UPLOAD_TO_ENDPOINTS.sh <project ID> local-kiosk kiosk_with_http
    ```

    This will create an API named
    "local-kiosk.endpoints.<project ID>.cloud.goog"

1.  Start the Endpoints proxy.

    ```bash
    ./endpoints/START_ENDPOINTS_PROXY.sh <project ID> local-kiosk <service account keyfile>
    ```

    This will create an API named
    "local-kiosk.endpoints.<project ID>.cloud.goog"

## On GCP

### Prerequisites

- You have a GCP project to use for the following steps.

All steps should be run from the root directory of this repository.

1.  Rebuild the protos with API descriptors for Endpoints to consume. These
    files will be output to the `generated/` directory.

    ```bash
    ./COMPILE_PROTOS.sh
    ```

1.  Upload your API to Endpoints, into your GCP project.

    ```bash
    ./endpoints/UPLOAD_TO_ENDPOINTS.sh <project ID> kiosk kiosk_with_http
    ```

    This will create an API named "kiosk.endpoints.<project ID>.cloud.goog"

1.  Start the VM.
