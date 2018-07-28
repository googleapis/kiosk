// Copyright 2016 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	google_protobuf "github.com/golang/protobuf/ptypes/empty"
	pb "github.com/googleapis/kiosk/generated"
	context "golang.org/x/net/context"
)

type DisplayServer struct {
	kiosks map[string]*pb.Kiosk
	signs map[string]*pb.Sign
	signsForKiosks map[string]string
	nextKioskId int
	nextSignId int
}

func NewDisplayServer() *DisplayServer {
 	return &DisplayServer{
		kiosks: make(map[string]*pb.Kiosk),
		signs: make(map[string]*pb.Sign),
		signsForKiosks: make(map[string]string),
		nextKioskId: 1,
		nextSignId: 1,
	}
}

// Create a kiosk. This enrolls the kiosk for sign display.
func (k *DisplayServer) CreateKiosk(c context.Context, kiosk *pb.Kiosk) (*pb.Kiosk, error) {
	return kiosk, nil
}

// List active kiosks.
func (k *DisplayServer) ListKiosks(c context.Context, x *google_protobuf.Empty) (*pb.ListKiosksResponse, error) {
	return nil, nil
}

// Get a kiosk.
func (k *DisplayServer) GetKiosk(c context.Context, r *pb.GetKioskRequest) (*pb.Kiosk, error) { return nil, nil }

// Delete a kiosk.
func (k *DisplayServer) DeleteKiosk(c context.Context, r *pb.DeleteKioskRequest) (*google_protobuf.Empty, error) { return nil, nil }

// Create a sign. This enrolls the sign for sign display.
func (k *DisplayServer) CreateSign(context.Context, *pb.Sign) (*pb.Sign, error) { return nil, nil }

// List active signs.
func (k *DisplayServer) ListSigns(context.Context, *google_protobuf.Empty) (*pb.ListSignsResponse, error) {
	return nil, nil
}

// Get a sign.
func (k *DisplayServer) GetSign(context.Context, *pb.GetSignRequest) (*pb.Sign, error) { return nil, nil }

// Delete a sign.
func (k *DisplayServer) DeleteSign(context.Context, *pb.DeleteSignRequest) (*google_protobuf.Empty, error) {
	return nil, nil
}

// Set a sign for display on one or more kiosks.
func (k *DisplayServer) SetSignForKiosks(context.Context, *pb.SetSignForKiosksRequest) (*google_protobuf.Empty, error) {
	return nil, nil
}

// Get the sign that should be displayed on a kiosk.
func (k *DisplayServer) GetSignForKiosk(context.Context, *pb.GetSignForKioskRequest) (*pb.Sign, error) { return nil, nil }

// Get signs that should be displayed on a kiosk. Streams.
func (k *DisplayServer) GetSignsForKiosk(*pb.GetSignForKioskRequest, pb.Display_GetSignsForKioskServer) error {
	return nil
}
