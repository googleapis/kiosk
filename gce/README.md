# Run the Kiosk API on Google Compute Engine

These scripts use the [gcloud](https://cloud.google.com/sdk/gcloud/)
command-line tool to configure a
[Google Compute Engine](https://cloud.google.com/compute/) instance to run the
Kiosk API.

### Use [SETUP.sh](SETUP.sh) to create the instance.

This will create an instance and invoke the [STARTUP.sh](STARTUP.sh) script,
which downloads dependencies and builds the Kiosk API server. The startup
script runs in the background after the instance has been created and takes a
few minutes to complete. Track its progress with `tail -f /var/log/syslog`.

### The server runs as root.

Inside the instance, the server is built and runs as root. If you connect
through the Google Cloud Console, you'll be signed in with your Google user id.
Change to root with `sudo su -`.

### Verify the server with the `k` tool and `go test`.

When the server is built, it will be automatically started. Connect to it by
setting the `KIOSK_SERVER` environment variable to the address of your instance
and using the `k` tool or by running `go test` from the root of this
repository. Note that `go test` is a destructive test - running it will delete
the signs and kiosks currently stored on the server and replace them with new
ones.

### Having connectivity problems?

If you are able to connect locally (from within your instance) but not
remotely, rerun the second command in SETUP.sh
(`gcloud compute firewall-rules create`). Give this a minute or two to
propagate, and then the firewall rules should be updated to allow the remote
connection.
