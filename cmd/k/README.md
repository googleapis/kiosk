# Control the Kiosk API with the k tool.

This directory contains a Go command-line tool that can be used to control the
Kiosk API. Running `k` with no arguments produces a list of commands.

## Configuration

By default, the `k` tool looks for an API server running at `localhost` on port
`8080`. Use the `KIOSK_SERVER` and `KIOSK_PORT` environment variables to
override this.

If the API server requires an API key, it can be set with the `KIOSK_APIKEY`
environment variable. These are sent as `x-api-key` header values.

Authorization tokens obtained with OAuth can be specified with the
`KIOSK_TOKEN` environment variable. These are sent as `Bearer` tokens in the
`Authorization` header.

## Sample

Here's an example using the `k` tool to interact with a Kiosk server running on
`localhost`. Note that commands are echoed by the `k` tool as they are
executed.

```
$ k list kiosks
FROM localhost:8080 LIST KIOSKS

$ k list signs
FROM localhost:8080 LIST SIGNS

$ k create sign sign-1
FROM localhost:8080 CREATE SIGN
id:1 name:"sign-1"

$ k create sign sign-2
FROM localhost:8080 CREATE SIGN
id:2 name:"sign-2"

$ k create kiosk kiosk-1
FROM localhost:8080 CREATE KIOSK <NAME>
id:1 name:"kiosk-1"

$ k list signs
FROM localhost:8080 LIST SIGNS
id:1 name:"sign-1"
id:2 name:"sign-2"

$ k list kiosks
FROM localhost:8080 LIST KIOSKS
id:1 name:"kiosk-1"

$ k set sign 1 for kiosk 1
FROM localhost:8080 SET SIGN <SIGN_ID> FOR KIOSK <KIOSK_ID>

$ k get sign for kiosk 1
FROM localhost:8080 GET SIGN FOR KIOSK <KIOSK_ID>
sign_id:1

$ k get signs for kiosk 1 ## this is a server-streaming gRPC call
FROM localhost:8080 GET SIGNS FOR KIOSK <KIOSK_ID>
sign_id:1 ## now run `k set sign 2 for kiosk 1` in another shell
sign_id:2 ## now run `k set sign 3 for kiosk 1` in another shell
sign_id:3
```
