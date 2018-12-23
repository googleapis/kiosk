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

import 'package:grpc/grpc.dart' as grpc;
import 'package:kiosk/src/generated/kiosk.pb.dart';
import 'package:kiosk/src/generated/kiosk.pbgrpc.dart';

import 'package:kiosk/src/generated/google/protobuf/empty.pb.dart';

class DisplayService extends DisplayServiceBase {
  createKiosk(call, request) {
    var completer = new Completer<Kiosk>();
    completer.complete(new Kiosk());
    return completer.future;
  }

  listKiosks(call, request) {
    var completer = new Completer<ListKiosksResponse>();
    completer.complete(new ListKiosksResponse());
    return completer.future;
  }

  getKiosk(call, request) {
    var completer = new Completer<Kiosk>();
    completer.complete(new Kiosk());
    return completer.future;
  }

  deleteKiosk(call, request) {
    var completer = new Completer<Empty>();
    completer.complete(new Empty());
    return completer.future;
  }

  createSign(call, request) {
    var completer = new Completer<Sign>();
    completer.complete(new Sign());
    return completer.future;
  }

  listSigns(call, request) {
    var completer = new Completer<ListSignsResponse>();
    completer.complete(new ListSignsResponse());
    return completer.future;
  }

  getSign(call, request) {
    var completer = new Completer<Sign>();
    completer.complete(new Sign());
    return completer.future;
  }

  deleteSign(call, request) {
    var completer = new Completer<Empty>();
    completer.complete(new Empty());
    return completer.future;
  }

  setSignIdForKioskIds(call, request) {
    var completer = new Completer<Empty>();
    completer.complete(new Empty());
    return completer.future;
  }

  getSignIdForKioskId(call, request) {
    var completer = new Completer<GetSignIdResponse>();
    completer.complete(new GetSignIdResponse());
    return completer.future;
  }

  getSignIdsForKioskId(call, request) async* {
    yield new GetSignIdResponse();
  }
}

class Server {
  Future<Null> main(List<String> args) async {
    final server = new grpc.Server([new DisplayService()]);
    await server.serve(port: 8081);
    print('Server listening on port ${server.port}...');
  }
}

main(List<String> args) async {
  await new Server().main(args);
}
