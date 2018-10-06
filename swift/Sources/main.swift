/*
 * Copyright 2017, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Commander
import Dispatch
import Foundation
import SwiftGRPC
import SwiftProtobuf

func buildServiceClient() -> Kiosk_DisplayServiceClient {
  var address = "localhost"
  var port = "8080"
  if let value = ProcessInfo.processInfo.environment["KIOSK_SERVER"] {
    address = value
  }
  if let value = ProcessInfo.processInfo.environment["KIOSK_PORT"] {
    port = value
  }
  return Kiosk_DisplayServiceClient(address: address + ":" + port, secure: false)
}

Group {
  
  $0.command("create-kiosk",  
             Argument<String>("name", description: "kiosk name"),
             description: "Create a kiosk.")
  { (name) in
    let service = buildServiceClient()
    var request = Kiosk_Kiosk()
    request.name = name
    let response = try service.createKiosk(request)
    print("\(response)")
  }
  
  $0.command("list-kiosks",
             description: "List kiosks.")
  {
    let service = buildServiceClient()
    let response = try service.listKiosks(SwiftProtobuf.Google_Protobuf_Empty())
    print("\(response)")
  }
  
  $0.command("get-kiosk",
             Argument<Int>("id", description: "kiosk id"),
             description: "Get a kiosk.")
  { (id) in
    let service = buildServiceClient()
    var request = Kiosk_GetKioskRequest()
    request.id = Int32(id)
    let response = try service.getKiosk(request)
    print("\(response)")
  }
  
  $0.command("delete-kiosk",
             Argument<Int>("id", description: "kiosk id"),
             description: "Delete a kiosk.")
  { (id) in
    let service = buildServiceClient()
    var request = Kiosk_DeleteKioskRequest()
    request.id = Int32(id)
    let response = try service.deleteKiosk(request)
    print("\(response)")
  }
  
  $0.command("create-sign",
             Argument<String>("name", description: "sign name"),
             Option("text", default: "", description: "text to display"),
             Option("image", default: "", description: "image file name"),
             description: "Create a sign.")
  { (name, text, image) in
    let service = buildServiceClient()
    var request = Kiosk_Sign()
    request.name = name
    request.text = text
    if image != "" {
      request.image = try Data(contentsOf: URL(fileURLWithPath:image))
    }
    let response = try service.createSign(request)
    print("\(response)")
  }
  
  $0.command("list-signs",
             description: "List signs.")
  {
    let service = buildServiceClient()
    var response = try service.listSigns(SwiftProtobuf.Google_Protobuf_Empty())
    for i in 0..<response.signs.count {
      if response.signs[i].image.count > 10 {
        response.signs[i].image = response.signs[i].image[0..<10]
      }
    }
    print("\(response)")
  }
  
  $0.command("get-sign",
             Argument<Int>("id", description: "Sign id"),
             description: "Get a sign.")
  { (id) in
    let service = buildServiceClient()
    var request = Kiosk_GetSignRequest()
    request.id = Int32(id)
    var response = try service.getSign(request)
    response.image = response.image[0..<10]
    print("\(response)")
  }
  
  $0.command("delete-sign",
             Argument<Int>("id", description: "sign id"),
             description: "Delete a sign.")
  { (id) in
    let service = buildServiceClient()
    var request = Kiosk_DeleteSignRequest()
    request.id = Int32(id)
    let response = try service.deleteSign(request)
    print("\(response)")
  }
  
  $0.command("set-sign",
             Argument<Int>("id", description: "sign id"),
             Option("kiosk", default: 0, description: "kiosk id"),
             description: "Set a sign for a kiosk (or all kiosks).")
  { (id, kiosk_id) in
    let service = buildServiceClient()
    var request = Kiosk_SetSignIdForKioskIdsRequest()
    request.signID = Int32(id)
    if kiosk_id != 0 {
      request.kioskIds = [Int32(kiosk_id)]
    }
    let response = try service.setSignIdForKioskIds(request)
    print("\(response)")
  }
  
  $0.command("get-sign-for-kiosk",
             Argument<Int>("id", description: "kiosk id"),
             description: "Get a sign for a kiosk.")
  { (id) in
    let service = buildServiceClient()
    var request = Kiosk_GetSignIdForKioskIdRequest()
    request.kioskID = Int32(id)
    let response = try service.getSignIdForKioskId(request)
    print("\(response)")
  }
  
  $0.command("get-signs-for-kiosk",
             Argument<Int>("id", description: "kiosk id"),
             description: "Get signs for a kiosk (streaming).")
  { (id) in
    let service = buildServiceClient()
    var request = Kiosk_GetSignIdForKioskIdRequest()
    request.kioskID = Int32(id)
    
    let sem = DispatchSemaphore(value: 0)
    var callResult : CallResult?
    let call = try service.getSignIdsForKioskId(request) { result in
      callResult = result
      sem.signal()
    }
    while true {
      guard let responseMessage = try call.receive()
        else { break }  // End of stream
      print("getSignIdsForKioskId received: \(responseMessage)")
    }
    _ = sem.wait()
    if let statusCode = callResult?.statusCode {
      print("getSignIdsForKioskId completed with code \(statusCode)")
    }
  }
  
  }.run()
