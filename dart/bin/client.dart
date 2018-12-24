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

import 'package:grpc/grpc.dart';
import 'package:kiosk/src/generated/kiosk.pb.dart';
import 'package:kiosk/src/generated/kiosk.pbgrpc.dart';

import 'package:kiosk/src/generated/google/protobuf/empty.pb.dart';

Future<Null> main(List<String> args) async {
  final channel = new ClientChannel('localhost',
      port: 8080,
      options: const ChannelOptions(
          credentials: const ChannelCredentials.insecure()));
  final stub = new DisplayClient(channel);

  try {
    final response = await stub.listKiosks(new Empty());
    print('Display client received: ${response}');
  } catch (e) {
    print('Caught error: $e');
  }
  await channel.shutdown();
}
