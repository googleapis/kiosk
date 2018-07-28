// Copyright 2018 Google Inc. All Rights Reserved.
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
	"errors"

	google_protobuf "github.com/golang/protobuf/ptypes/empty"
	pb "github.com/googleapis/kiosk/generated"
	context "golang.org/x/net/context"
)

type DisplayServer struct {
	kiosks         []*pb.Kiosk
	signs          []*pb.Sign
	signsForKiosks map[string]string
	nextKioskId    int32
	nextSignId     int32
}

func NewDisplayServer() *DisplayServer {
	return &DisplayServer{
		kiosks:         make([]*pb.Kiosk, 1000, 1000),
		signs:          make([]*pb.Sign, 1000, 1000),
		signsForKiosks: make(map[string]string),
		nextKioskId:    1,
		nextSignId:     1,
	}
}

// Create a kiosk. This enrolls the kiosk for sign display.
func (s *DisplayServer) CreateKiosk(c context.Context, kiosk *pb.Kiosk) (*pb.Kiosk, error) {
	kiosk.Id = s.nextKioskId
	s.kiosks[kiosk.Id] = kiosk
	s.nextKioskId++
	return kiosk, nil
}

// List active kiosks.
func (s *DisplayServer) ListKiosks(c context.Context, x *google_protobuf.Empty) (*pb.ListKiosksResponse, error) {
	response := &pb.ListKiosksResponse{}
	for _, k := range s.kiosks {
		if k != nil {
			response.Kiosks = append(response.Kiosks, k)
		}
	}
	return response, nil
}

// Get a kiosk.
func (s *DisplayServer) GetKiosk(c context.Context, r *pb.GetKioskRequest) (*pb.Kiosk, error) {
	i := r.Id
	if i >= 0 && i < int32(len(s.kiosks)) && s.kiosks[i] != nil {
		return s.kiosks[i], nil
	} else {
		return nil, errors.New("invalid kiosk id")
	}
}

// Delete a kiosk.
func (s *DisplayServer) DeleteKiosk(c context.Context, r *pb.DeleteKioskRequest) (*google_protobuf.Empty, error) {
	i := r.Id
	if i >= 0 && i < int32(len(s.kiosks)) && s.kiosks[i] != nil {
		s.kiosks[i] = nil
		return &google_protobuf.Empty{}, nil
	} else {
		return nil, errors.New("invalid kiosk id")
	}
}

// Create a sign. This enrolls the sign for sign display.
func (s *DisplayServer) CreateSign(context.Context, *pb.Sign) (*pb.Sign, error) { return nil, nil }

// List active signs.
func (s *DisplayServer) ListSigns(context.Context, *google_protobuf.Empty) (*pb.ListSignsResponse, error) {
	return nil, nil
}

// Get a sign.
func (s *DisplayServer) GetSign(context.Context, *pb.GetSignRequest) (*pb.Sign, error) {
	return nil, nil
}

// Delete a sign.
func (s *DisplayServer) DeleteSign(context.Context, *pb.DeleteSignRequest) (*google_protobuf.Empty, error) {
	return nil, nil
}

// Set a sign for display on one or more kiosks.
func (s *DisplayServer) SetSignForKiosks(context.Context, *pb.SetSignForKiosksRequest) (*google_protobuf.Empty, error) {
	return nil, nil
}

// Get the sign that should be displayed on a kiosk.
func (s *DisplayServer) GetSignForKiosk(context.Context, *pb.GetSignForKioskRequest) (*pb.Sign, error) {
	return nil, nil
}

// Get signs that should be displayed on a kiosk. Streams.
func (s *DisplayServer) GetSignsForKiosk(*pb.GetSignForKioskRequest, pb.Display_GetSignsForKioskServer) error {
	return nil
}
