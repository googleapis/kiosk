# Swift Kiosk samples

This directory contains a Swift implementation of a Kiosk API server and a
command-line tool that can be used to control the Kiosk API. Build both with
`make`.

Samples were verified with Apple Swift version 4.1.2 on OS X.

## Usage

By default, both the server (`kiosk-server-swift`) and command-line tool
(`kiosk-tool-swift`) expect the API server to be running at `localhost` on port
`8080`. Use the `KIOSK_SERVER` and `KIOSK_PORT` environment variables to
override this.

As a simple sanity check, start the server with `kiosk-server-swift`, and then
call it with the following commands:

```
$ kiosk-tool-swift create-kiosk one
kiosk_tool_swift.Kiosk_Kiosk:
id: 1
name: "one"

$ kiosk-tool-swift create-kiosk two
kiosk_tool_swift.Kiosk_Kiosk:
id: 2
name: "two"

$ kiosk-tool-swift list-kiosks
kiosk_tool_swift.Kiosk_ListKiosksResponse:
kiosks {
  id: 1
  name: "one"
}
kiosks {
  id: 2
  name: "two"
}
```
