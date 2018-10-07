/*
 * Copyright 2018, gRPC Authors All rights reserved.
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
import SwiftGRPC
import SwiftProtobuf

class KioskProvider : Kiosk_DisplayProvider {
  var sessionLifetime    : Int64
  var kiosks             : [Int32: Kiosk_Kiosk]
  var signs              : [Int32: Kiosk_Sign]
  var signIdsForKioskIds : [Int32: Int32]
  // var subscribers        : map[int32]map[chan int32]bool
  var nextKioskId        : Int32
  var nextSignId         : Int32
  // var mux                :sync.Mutex
  
  init() {
    self.sessionLifetime = 24 * 60 * 3600; // 24 hours
    self.kiosks = [:]
    self.signs = [:]
    self.signIdsForKioskIds = [:]
    self.nextKioskId = 1
    self.nextSignId = 1
  }
  
  func createKiosk(request: Kiosk_Kiosk, session: Kiosk_DisplayCreateKioskSession) throws
    -> Kiosk_Kiosk {
      return Kiosk_Kiosk()
  }
  
  func listKiosks(request: SwiftProtobuf.Google_Protobuf_Empty, session: Kiosk_DisplayListKiosksSession) throws
    -> Kiosk_ListKiosksResponse {
      return Kiosk_ListKiosksResponse()
  }
  
  func getKiosk(request: Kiosk_GetKioskRequest, session: Kiosk_DisplayGetKioskSession) throws
    -> Kiosk_Kiosk {
      return Kiosk_Kiosk()
  }
  
  func deleteKiosk(request: Kiosk_DeleteKioskRequest, session: Kiosk_DisplayDeleteKioskSession) throws
    -> SwiftProtobuf.Google_Protobuf_Empty {
      return SwiftProtobuf.Google_Protobuf_Empty()
  }
  
  func createSign(request: Kiosk_Sign, session: Kiosk_DisplayCreateSignSession) throws
    -> Kiosk_Sign {
      return Kiosk_Sign()
  }
  
  func listSigns(request: SwiftProtobuf.Google_Protobuf_Empty, session: Kiosk_DisplayListSignsSession) throws
    -> Kiosk_ListSignsResponse {
      return Kiosk_ListSignsResponse()
  }
  
  func getSign(request: Kiosk_GetSignRequest, session: Kiosk_DisplayGetSignSession) throws
    -> Kiosk_Sign {
      return Kiosk_Sign()
  }
  
  func deleteSign(request: Kiosk_DeleteSignRequest, session: Kiosk_DisplayDeleteSignSession) throws
    -> SwiftProtobuf.Google_Protobuf_Empty {
      return SwiftProtobuf.Google_Protobuf_Empty()
  }
  
  func setSignIdForKioskIds(request: Kiosk_SetSignIdForKioskIdsRequest, session: Kiosk_DisplaySetSignIdForKioskIdsSession) throws
    -> SwiftProtobuf.Google_Protobuf_Empty {
      return SwiftProtobuf.Google_Protobuf_Empty()
  }
  
  func getSignIdForKioskId(request: Kiosk_GetSignIdForKioskIdRequest, session: Kiosk_DisplayGetSignIdForKioskIdSession) throws
    -> Kiosk_GetSignIdResponse {
      return Kiosk_GetSignIdResponse()
  }
  
  func getSignIdsForKioskId(request: Kiosk_GetSignIdForKioskIdRequest, session: Kiosk_DisplayGetSignIdsForKioskIdSession) throws
    -> ServerStatus? {
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

let sem = DispatchSemaphore(value: 0)
var kioskServer: ServiceServer?
print("starting server")
kioskServer = ServiceServer(address: address + ":" + port, serviceProviders: [KioskProvider()])
kioskServer?.start()

// This blocks to keep the main thread from finishing while the server runs,
// but the server never exits. Kill the process to stop it.
_ = sem.wait()
