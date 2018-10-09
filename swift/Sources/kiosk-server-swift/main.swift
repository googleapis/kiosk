/*
 * Copyright 2018, Google LLC. All rights reserved.
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

import Dispatch
import Foundation
import SwiftProtobuf
import SwiftGRPC

class KioskProvider : Kiosk_DisplayProvider {
  var kiosks             : [Int32: Kiosk_Kiosk]
  var signs              : [Int32: Kiosk_Sign]
  var signIdsForKioskIds : [Int32: Int32]
  var subscribers        : Set<DispatchSemaphore>
  var nextKioskId        : Int32
  var nextSignId         : Int32
  
  init() {
    self.kiosks = [:]
    self.signs = [:]
    self.signIdsForKioskIds = [:]
    self.subscribers = []
    self.nextKioskId = 1
    self.nextSignId = 1
  }
  
  func createKiosk(request: Kiosk_Kiosk, session: Kiosk_DisplayCreateKioskSession) throws
    -> Kiosk_Kiosk {
      var kiosk = request
      kiosk.id = self.nextKioskId
      self.nextKioskId += 1
      self.kiosks[kiosk.id] = kiosk
      return kiosk
  }
  
  func listKiosks(request: SwiftProtobuf.Google_Protobuf_Empty, session: Kiosk_DisplayListKiosksSession) throws
    -> Kiosk_ListKiosksResponse {
      var k = Array(self.kiosks.values)
      k.sort(by:{(a: Kiosk_Kiosk, b: Kiosk_Kiosk) in a.id < b.id})
      var response = Kiosk_ListKiosksResponse()
      response.kiosks = k
      return response
  }
  
  func getKiosk(request: Kiosk_GetKioskRequest, session: Kiosk_DisplayGetKioskSession) throws
    -> Kiosk_Kiosk {
      return self.kiosks[request.id]!
  }
  
  func deleteKiosk(request: Kiosk_DeleteKioskRequest, session: Kiosk_DisplayDeleteKioskSession) throws
    -> SwiftProtobuf.Google_Protobuf_Empty {
      self.kiosks[request.id] = nil
      if self.kiosks.count == 0 {
        self.nextKioskId = 1
      }
      return SwiftProtobuf.Google_Protobuf_Empty()
  }
  
  func createSign(request: Kiosk_Sign, session: Kiosk_DisplayCreateSignSession) throws
    -> Kiosk_Sign {
      var sign = request
      sign.id = self.nextSignId
      self.nextSignId += 1
      self.signs[sign.id] = sign
      return sign
  }
  
  func listSigns(request: SwiftProtobuf.Google_Protobuf_Empty, session: Kiosk_DisplayListSignsSession) throws
    -> Kiosk_ListSignsResponse {
      var s = Array(self.signs.values)
      s.sort(by:{(a: Kiosk_Sign, b: Kiosk_Sign) in a.id < b.id})
      var response = Kiosk_ListSignsResponse()
      response.signs = s
      return response
  }
  
  func getSign(request: Kiosk_GetSignRequest, session: Kiosk_DisplayGetSignSession) throws
    -> Kiosk_Sign {
      return self.signs[request.id]!
  }
  
  func deleteSign(request: Kiosk_DeleteSignRequest, session: Kiosk_DisplayDeleteSignSession) throws
    -> SwiftProtobuf.Google_Protobuf_Empty {
      self.signs[request.id] = nil
      if self.signs.count == 0 {
        self.nextSignId = 1
      }
      return SwiftProtobuf.Google_Protobuf_Empty()
  }
  
  func setSignIdForKioskIds(request: Kiosk_SetSignIdForKioskIdsRequest, session: Kiosk_DisplaySetSignIdForKioskIdsSession) throws
    -> SwiftProtobuf.Google_Protobuf_Empty {
      if request.kioskIds.count > 0 {
        for id in request.kioskIds {
          self.signIdsForKioskIds[id] = request.signID
        }
      } else {
        for id in self.kiosks.keys {
          self.signIdsForKioskIds[id] = request.signID
        }
      }
      for sem in self.subscribers {
        sem.signal()
      }
      return SwiftProtobuf.Google_Protobuf_Empty()
  }
  
  func getSignIdForKioskId(request: Kiosk_GetSignIdForKioskIdRequest, session: Kiosk_DisplayGetSignIdForKioskIdSession) throws
    -> Kiosk_GetSignIdResponse {
      var response = Kiosk_GetSignIdResponse()
      if let signID = self.signIdsForKioskIds[request.kioskID] {
        response.signID = signID
      }
      return response
  }
  
  func getSignIdsForKioskId(request: Kiosk_GetSignIdForKioskIdRequest, session: Kiosk_DisplayGetSignIdsForKioskIdSession) throws
    -> ServerStatus? {
      let update_semaphore = DispatchSemaphore(value: 0)
      self.subscribers.insert(update_semaphore)
      var running = true
      while running {
        var response = Kiosk_GetSignIdResponse()
        if let signID = self.signIdsForKioskIds[request.kioskID] {
          response.signID = signID
        }
        let send_semaphore = DispatchSemaphore(value: 0)
        try session.send(response) {
          if let error = $0 {
            print("send error: \(error)")
            running = false
          }
          send_semaphore.signal()
        }
        send_semaphore.wait()
        if !running {
          break
        }
        update_semaphore.wait()
      }
      print("unsubscribing")
      self.subscribers.remove(update_semaphore)
      return .ok
  }
}

var address = "localhost"
var port = "8080"
if let value = ProcessInfo.processInfo.environment["KIOSK_SERVER"] {
  address = value
}
if let value = ProcessInfo.processInfo.environment["KIOSK_PORT"] {
  port = value
}

print("starting server")
let sem = DispatchSemaphore(value: 0)
let kioskServer = ServiceServer(address: address + ":" + port, serviceProviders: [KioskProvider()])
kioskServer.start()
_ = sem.wait()

// Now we are blocked to keep the main thread from finishing while the server runs,
// but the server never exits. Kill the process to stop it.
