# Control the Kiosk API with the k tool.

This directory contains a Go command-line tool that can be used to control
the Kiosk API. Running `k` with no arguments produces a list of commands.

## Sample

Here's an example using the `k` tool to interact with a Kiosk server
running on localhost. Note that commands are echoed by the `k` tool
as they are executed.

```
$ k list kiosks
list kiosks

$ k list signs
list signs

$ k create sign sign-1
create sign
id:1 name:"sign-1" 

$ k create sign sign-2
create sign
id:2 name:"sign-2" 

$ k create kiosk kiosk-1
create kiosk <name>
id:1 name:"kiosk-1" 

$ k list signs
list signs
id:1 name:"sign-1" 
id:2 name:"sign-2" 

$ k list kiosks
list kiosks
id:1 name:"kiosk-1" 

$ k set sign 1 for kiosk 1
set sign <sign_id> for kiosk <kiosk_id>

$ k get sign for kiosk 1
get sign for kiosk <kiosk_id>
sign_id:1 

$ k get signs for kiosk 1 ## this is a server-streaming gRPC call
get signs for kiosk <kiosk_id>
sign_id:1 ## now run `k set sign 2 for kiosk 1` in another shell
sign_id:2 ## now run `k set sign 3 for kiosk 1` in another shell
sign_id:3 
```
