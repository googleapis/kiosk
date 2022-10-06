# Dart Kiosk samples

This directory contains Dart sample code related to the Kiosk API.

## Usage:

- Install the Dart protoc plugin:

  - `pub global activate protoc_plugin`.
  - add `.pub-cache/bin` in your home dir to your PATH if you haven't already.

- Run `COMPILE-PROTOS.sh` to download additional sources and compile needed
  support code.
- Run `pub get` to download Dart dependencies.
- Run `dart bin/server.dart` to run the sample server.
- Run `dart bin/client.dart` to run the sample client.
