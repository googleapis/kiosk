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
import SwiftGRPC
import SwiftProtobuf

typealias KioskID = Int32
typealias SignID = Int32
typealias SubscriberID = Int32

class KioskProvider: Kiosk_DisplayProvider {
  let queue = DispatchQueue(label: "KioskProvider.lock")
  
  var kiosks: [KioskID: Kiosk_Kiosk] = [:]
  var signs: [SignID: Kiosk_Sign] = [:]
  var signIdsForKioskIds: [KioskID: SignID] = [:]
  // `SignID == nil` in this case indicates that the corresponding kiosk has been deleted.
  var subscribersForKioskIds: [KioskID: [SubscriberID: (SignID?) -> Void]] = [:]
  var nextKioskId: KioskID = 1
  var nextSignId: SignID = 1
  var nextSubscriberId: SubscriberID = 1
  
  func createKiosk(request: Kiosk_Kiosk,
                   session: Kiosk_DisplayCreateKioskSession) throws ->
    Kiosk_Kiosk {
      return queue.sync {
        var kiosk = request
        kiosk.id = self.nextKioskId
        self.nextKioskId += 1
        self.kiosks[kiosk.id] = kiosk
        return kiosk
      }
  }
  
  func listKiosks(request: SwiftProtobuf.Google_Protobuf_Empty,
                  session: Kiosk_DisplayListKiosksSession) throws ->
    Kiosk_ListKiosksResponse {
      return queue.sync {
        var k = Array(self.kiosks.values)
        k.sort(by: { (a: Kiosk_Kiosk, b: Kiosk_Kiosk) in a.id < b.id })
        var response = Kiosk_ListKiosksResponse()
        response.kiosks = k
        return response
      }
  }
  
  func getKiosk(request: Kiosk_GetKioskRequest,
                session: Kiosk_DisplayGetKioskSession) throws ->
    Kiosk_Kiosk {
      return try queue.sync {
        if let kiosk = self.kiosks[request.id] {
          return kiosk
        } else {
          throw ServerStatus(code: .notFound, message: "No kiosk with that ID found.")
        }
      }
  }
  
  func deleteKiosk(request: Kiosk_DeleteKioskRequest,
                   session: Kiosk_DisplayDeleteKioskSession) throws ->
    SwiftProtobuf.Google_Protobuf_Empty {
      return queue.sync {
        self.kiosks.removeValue(forKey: request.id)
        self.signIdsForKioskIds.removeValue(forKey: request.id)
        
        for block in self.subscribersForKioskIds[request.id, default: [:]].values {
          // Notify all subscribers that this kiosk has been deleted, so that they can end the call.
          block(nil)
        }
        self.subscribersForKioskIds.removeValue(forKey: request.id)
        return SwiftProtobuf.Google_Protobuf_Empty()
      }
  }
  
  func createSign(request: Kiosk_Sign,
                  session: Kiosk_DisplayCreateSignSession) throws ->
    Kiosk_Sign {
      return queue.sync {
        var sign = request
        sign.id = self.nextSignId
        self.nextSignId += 1
        self.signs[sign.id] = sign
        return sign
      }
  }
  
  func listSigns(request: SwiftProtobuf.Google_Protobuf_Empty,
                 session: Kiosk_DisplayListSignsSession) throws ->
    Kiosk_ListSignsResponse {
      return queue.sync {
        var s = Array(self.signs.values)
        s.sort(by: { (a: Kiosk_Sign, b: Kiosk_Sign) in a.id < b.id })
        var response = Kiosk_ListSignsResponse()
        response.signs = s
        return response
      }
  }
  
  func getSign(request: Kiosk_GetSignRequest,
               session: Kiosk_DisplayGetSignSession) throws ->
    Kiosk_Sign {
      return try queue.sync {
        if let sign = self.signs[request.id] {
          return sign
        } else {
          throw ServerStatus(code: .notFound, message: "No sign with that ID found.")
        }
      }
  }
  
  func deleteSign(request: Kiosk_DeleteSignRequest,
                  session: Kiosk_DisplayDeleteSignSession) throws ->
    SwiftProtobuf.Google_Protobuf_Empty {
      return queue.sync {
        self.signs.removeValue(forKey: request.id)
        return SwiftProtobuf.Google_Protobuf_Empty()
      }
  }
  
  func setSignIdForKioskIds(request: Kiosk_SetSignIdForKioskIdsRequest,
                            session: Kiosk_DisplaySetSignIdForKioskIdsSession) throws ->
    SwiftProtobuf.Google_Protobuf_Empty {
      return try queue.sync {
        if self.signs[request.signID] == nil {
          throw ServerStatus(code: .notFound, message: "No sign with that ID found.")
        }
        
        let kioskIDsToChange = !request.kioskIds.isEmpty
          ? request.kioskIds
          : Array(self.kiosks.keys)
        for id in kioskIDsToChange {
          self.signIdsForKioskIds[id] = request.signID

          for block in self.subscribersForKioskIds[id, default: [:]].values {
            block(request.signID)
          }
        }
        return SwiftProtobuf.Google_Protobuf_Empty()
      }
  }
  
  func getSignIdForKioskId(request: Kiosk_GetSignIdForKioskIdRequest,
                           session: Kiosk_DisplayGetSignIdForKioskIdSession) throws ->
    Kiosk_GetSignIdResponse {
      return queue.sync {
        var response = Kiosk_GetSignIdResponse()
        if let signID = self.signIdsForKioskIds[request.kioskID] {
          response.signID = signID
        }
        return response
      }
  }
  
  func getSignIdsForKioskId(request: Kiosk_GetSignIdForKioskIdRequest,
                            session: Kiosk_DisplayGetSignIdsForKioskIdSession) throws ->
    ServerStatus? {
      try queue.sync {
        if self.kiosks[request.kioskID] == nil {
          throw ServerStatus(code: .notFound, message: "No kiosk with that ID found.")
        }
        
        let currentSubscriberId = nextSubscriberId
        nextSubscriberId += 1
        
        let removeSubscriberAndCloseSession: (String) -> Void = { [weak self] reason in
          print("unsubscribing subscriber \(currentSubscriberId) for kiosk \(request.kioskID), reason: \(reason)")
          self?.subscribersForKioskIds[request.kioskID, default: [:]].removeValue(forKey: currentSubscriberId)
          try? session.close(withStatus: .ok, completion: nil)
        }
        let subscriberBlock: (Int32?) -> Void = { [weak self] newSignID in
          guard let newSignID = newSignID else {
            // Indicates that the corresponding Kiosk has been deleted; delete the subscriber block and end the call.
            // No `queue.sync` needed here, as this block is always called from within a `queue.sync` block already.
            removeSubscriberAndCloseSession("kiosk deleted")
            return
          }
          
          var response = Kiosk_GetSignIdResponse()
          response.signID = newSignID
          do {
            try session.send(response) {
              if let error = $0 {
                print("send error: \(error)")
                self?.queue.sync {
                  // Indicates that the corresponding call has been closed client-side (or we are having connection
                  // problems); delete the subscriber block and end the call.
                  // `queue.sync` needed here, as this block is called asynchronously.
                  removeSubscriberAndCloseSession("sending error, call was probably ended client-side")
                }
              }
            }
          } catch {
            // No `queue.sync` needed here, as this block is always called from within a `queue.sync` block already.
            removeSubscriberAndCloseSession("unexpected sending error")
          }
        }
        
        print("subscribing subscriber \(currentSubscriberId) for kiosk \(request.kioskID)")
        self.subscribersForKioskIds[request.kioskID, default: [:]][currentSubscriberId] = subscriberBlock
        // Start the stream by sending the current sign ID.
        subscriberBlock(self.signIdsForKioskIds[request.kioskID] ?? 0)
      }
      
      return nil  // Status is sent in `subscriberBlock`.
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
