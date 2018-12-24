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

import 'dart:async';
import 'dart:isolate';

import 'package:grpc/grpc.dart' as grpc;
import 'package:kiosk/src/generated/kiosk.pb.dart';
import 'package:kiosk/src/generated/kiosk.pbgrpc.dart';

import 'package:kiosk/src/generated/google/protobuf/empty.pb.dart';

enum Command {
  create_kiosk,
  list_kiosks,
  get_kiosk,
  delete_kiosk,
  create_sign,
  list_signs,
  get_sign,
  delete_sign,
  set_sign_id_for_kiosk_ids,
  get_sign_id_for_kiosk_id,
  get_sign_ids_for_kiosk_ids
}

// Manage all service data in an in-memory data store.
// Runs in a separate thread.
void manage(SendPort listener) async {
  // initialize in-memory data store
  var kiosks = <int, Kiosk>{};
  var signs = <int, Sign>{};
  var nextKioskId = 1;
  var nextSignId = 1;

  // create receive port and send it to the listener
  var port = new ReceivePort();
  listener.send(port.sendPort);

  // listen on port for commands
  await for (var msg in port) {
    var command = msg[0];
    var data = msg[1];
    print('do ${command} with ${data}');
    SendPort replyTo = msg[2];

    switch (command) {
      case Command.create_kiosk:
        // assign an id and save the kiosk.
        var kiosk = data;
        kiosk.id = nextKioskId++;
        kiosks[kiosk.id] = kiosk;
        // reply with the new Kiosk.
        replyTo.send(kiosk);
        break;
      case Command.list_kiosks:
        // reply with a list of all kiosks.
        replyTo.send(kiosks.values.toList());
        break;
      case Command.get_kiosk:
        // lookup the kiosk.
        var kiosk = kiosks[data.id];
        // reply with the requested kiosk or null.
        replyTo.send(kiosk);
        break;
      case Command.delete_kiosk:
        // delete the kiosk.
        kiosks.remove(data.id);
        if (kiosks.isEmpty) {
          nextKioskId = 1;
        }
        // reply with the Empty() message
        replyTo.send(Empty());
        break;
      default:
        replyTo.send(data);
    }
  }
}

/// sends a message on a port, receives the response,
/// and returns the message that was received
Future sendReceive(SendPort port, command, msg) {
  ReceivePort response = new ReceivePort();
  port.send([command, msg, response.sendPort]);
  return response.first;
}

class DisplayService extends DisplayServiceBase {
  SendPort dataManagerSendPort;

  DisplayService(this.dataManagerSendPort);

  createKiosk(call, request) async {
    // FIXME: This should be a direct return (no "then"),
    // but sendReceive() returns Future<dynamic> and we need FutureOr<Kiosk>.
    return sendReceive(dataManagerSendPort, Command.create_kiosk, request)
        .then((msg) {
      return msg;
    });
  }

  listKiosks(call, request) async {
    return sendReceive(dataManagerSendPort, Command.list_kiosks, request)
        .then((msg) {
      return ListKiosksResponse()..kiosks.addAll(msg);
    });
  }

  getKiosk(call, request) async {
    // FIXME: This should be a direct return.
    return sendReceive(dataManagerSendPort, Command.get_kiosk, request)
        .then((msg) {
      return msg;
    });
  }

  deleteKiosk(call, request) async {
    // FIXME: This should be a direct return.
    return sendReceive(dataManagerSendPort, Command.delete_kiosk, request)
        .then((msg) {
      return msg;
    });
  }

  createSign(call, request) async {
    var msg =
        await sendReceive(dataManagerSendPort, Command.create_sign, request);
    var completer = new Completer<Sign>();
    completer.complete(new Sign());
    return completer.future;
  }

  listSigns(call, request) async {
    var msg =
        await sendReceive(dataManagerSendPort, Command.list_signs, request);
    var completer = new Completer<ListSignsResponse>();
    completer.complete(new ListSignsResponse());
    return completer.future;
  }

  getSign(call, request) async {
    var msg = await sendReceive(dataManagerSendPort, Command.get_sign, request);
    var completer = new Completer<Sign>();
    completer.complete(new Sign());
    return completer.future;
  }

  deleteSign(call, request) async {
    var msg =
        await sendReceive(dataManagerSendPort, Command.delete_sign, request);
    var completer = new Completer<Empty>();
    completer.complete(new Empty());
    return completer.future;
  }

  setSignIdForKioskIds(call, request) async {
    var msg = await sendReceive(
        dataManagerSendPort, Command.set_sign_id_for_kiosk_ids, request);
    var completer = new Completer<Empty>();
    completer.complete(new Empty());
    return completer.future;
  }

  getSignIdForKioskId(call, request) async {
    var msg = await sendReceive(
        dataManagerSendPort, Command.get_sign_id_for_kiosk_id, request);
    var completer = new Completer<GetSignIdResponse>();
    completer.complete(new GetSignIdResponse());
    return completer.future;
  }

  getSignIdsForKioskId(call, request) async* {
    yield new GetSignIdResponse();
  }
}

main(List<String> args) async {
  // Start the data manager and wait for it to reply with its SendPort.
  var receivePort = new ReceivePort();
  await Isolate.spawn(manage, receivePort.sendPort);
  var dataManagerSendPort = await receivePort.first;

  // Run the gRPC server.
  final server = new grpc.Server([new DisplayService(dataManagerSendPort)]);
  await server.serve(port: 8080);
  print('Server listening on port ${server.port}...');
}
