// Copyright (c) 2018, the gRPC project authors. Please see the AUTHORS file
// for details. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/// Dart implementation of the gRPC Kiosk Display client.
import 'dart:async';
import 'dart:io';

import 'package:args/args.dart';
import 'package:grpc/grpc.dart';
import 'package:kiosk/src/generated/kiosk.pb.dart';
import 'package:kiosk/src/generated/kiosk.pbgrpc.dart';
import 'package:kiosk/src/generated/google/protobuf/empty.pb.dart';

Future<Null> main(List<String> args) async {
  if (args.length == 0) {
    print('Usage: ...');
    exit(0);
  }
  var command = args[0];
  var parser = new ArgParser();
  switch (command) {
    case "list-kiosks":
    case "list-signs":
      break;
    case "create-kiosk":
      parser.addOption('name');
      break;
    case "create-sign":
      parser.addOption('name');
      parser.addOption('text');
      parser.addOption('image');
      break;
    case "delete-kiosk":
    case "delete-sign":
    case "get-kiosk":
    case "get-sign":
    case "get-sign-id-for-kiosk-id":
    case "get-sign-ids-for-kiosk-id":
      parser.addOption('id');
      break;
    case "set-sign-id-for-kiosk-ids":
      parser.addOption('sign_id');
      parser.addOption('kiosk_id');
      break;
    default:
      print('Unknown command: $command');
      exit(-1);
  }

  var results;
  try {
    results = parser.parse(args.sublist(1));
  } catch (e) {
    print('$e');
    exit(-1);
  }

  final channel = new ClientChannel('localhost',
      port: 8080,
      options: const ChannelOptions(
          credentials: const ChannelCredentials.insecure()));

  final channelCompleter = Completer<void>();
  ProcessSignal.sigint.watch().listen((_) async {
    print("sigint begin");
    await channel.terminate();
    channelCompleter.complete();
    print("sigint end");
  });

  final stub = new DisplayClient(channel);

  try {
    switch (command) {
      case "list-kiosks":
        final response = await stub.listKiosks(new Empty());
        print('Received: ${response}');
        break;
      case "list-signs":
        final response = await stub.listSigns(new Empty());
        print('Received: ${response}');
        break;
      case "create-kiosk":
        var request = Kiosk();
        request.name = results['name'];
        final response = await stub.createKiosk(request);
        print('Received: ${response}');
        break;
      case "create-sign":
        var request = Sign();
        request.name = results['name'];
        if (results['text'] != null) {
          request.text = results['text'];
        }
        if (results['image'] != null) {
          // this should actually be the bytes of the file.
          request.image = results['image'];
        }
        final response = await stub.createSign(request);
        print('Received: ${response}');
        break;
      case "delete-kiosk":
        var request = DeleteKioskRequest();
        request.id = int.parse(results['id']);
        final response = await stub.deleteKiosk(request);
        print('Received: ${response}');
        break;
      case "delete-sign":
        var request = DeleteSignRequest();
        request.id = int.parse(results['id']);
        ;
        final response = await stub.deleteSign(request);
        print('Received: ${response}');
        break;
      case "get-kiosk":
        var request = GetKioskRequest();
        request.id = int.parse(results['id']);
        final response = await stub.getKiosk(request);
        print('Received: ${response}');
        break;
      case "get-sign":
        var request = GetSignRequest();
        request.id = int.parse(results['id']);
        final response = await stub.getSign(request);
        print('Received: ${response}');
        break;
      case "get-sign-id-for-kiosk-id":
        var request = GetSignIdForKioskIdRequest();
        request.kioskId = int.parse(results['id']);
        final response = await stub.getSignIdForKioskId(request);
        print('Received: ${response}');
        break;
      case "get-sign-ids-for-kiosk-id":
        var request = GetSignIdForKioskIdRequest();
        request.kioskId = int.parse(results['id']);
        var stream = stub.getSignIdsForKioskId(request);
        stream.listen(print);
        await channelCompleter.future;
        exit(0);
        break;
      case "set-sign-id-for-kiosk-ids":
        var request = SetSignIdForKioskIdsRequest();
        request.signId = int.parse(results['sign_id']);
        request.kioskIds.add(int.parse(results['kiosk_id']));
        final response = await stub.setSignIdForKioskIds(request);
        print('Received: ${response}');
        break;
      default:
        break;
    }
  } catch (e) {
    print('Caught error: $e');
  }
  await channel.shutdown();
  exit(0);
}
