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
	kiosks             []*pb.Kiosk
	signs              []*pb.Sign
	signIdsForKioskIds []int32
	nextKioskId        int32
	nextSignId         int32
}

func NewDisplayServer() *DisplayServer {
	return &DisplayServer{
		kiosks:             make([]*pb.Kiosk, 1000, 1000),
		signs:              make([]*pb.Sign, 1000, 1000),
		signIdsForKioskIds: make([]int32, 1000, 1000),
		nextKioskId:        1,
		nextSignId:         1,
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
func (s *DisplayServer) CreateSign(c context.Context, sign *pb.Sign) (*pb.Sign, error) {
	sign.Id = s.nextSignId
	s.signs[sign.Id] = sign
	s.nextSignId++
	return sign, nil
}

// List active signs.
func (s *DisplayServer) ListSigns(c context.Context, x *google_protobuf.Empty) (*pb.ListSignsResponse, error) {
	response := &pb.ListSignsResponse{}
	for _, k := range s.signs {
		if k != nil {
			response.Signs = append(response.Signs, k)
		}
	}
	return response, nil
}

// Get a sign.
func (s *DisplayServer) GetSign(c context.Context, r *pb.GetSignRequest) (*pb.Sign, error) {
	i := r.Id
	if i >= 0 && i < int32(len(s.signs)) && s.signs[i] != nil {
		return s.signs[i], nil
	} else {
		return nil, errors.New("invalid sign id")
	}
}

// Delete a sign.
func (s *DisplayServer) DeleteSign(c context.Context, r *pb.DeleteSignRequest) (*google_protobuf.Empty, error) {
	i := r.Id
	if i >= 0 && i < int32(len(s.signs)) && s.signs[i] != nil {
		s.signs[i] = nil
		return &google_protobuf.Empty{}, nil
	} else {
		return nil, errors.New("invalid sign id")
	}
}

// Set a sign for display on one or more kiosks.
func (s *DisplayServer) SetSignIdForKioskIds(c context.Context, r *pb.SetSignIdForKioskIdsRequest) (*google_protobuf.Empty, error) {
	for _, kiosk_id := range r.KioskIds {
		s.signIdsForKioskIds[kiosk_id] = r.SignId
	}
	if len(r.KioskIds) == 0 {
		for kiosk_id, _ := range s.signIdsForKioskIds {
			s.signIdsForKioskIds[kiosk_id] = r.SignId
		}
	}
	return &google_protobuf.Empty{}, nil
}

// Get the sign that should be displayed on a kiosk.
func (s *DisplayServer) GetSignIdForKioskId(c context.Context, r *pb.GetSignIdForKioskIdRequest) (*pb.GetSignIdResponse, error) {
	kiosk_id := r.KioskId
	sign_id := s.signIdsForKioskIds[kiosk_id]
	response := &pb.GetSignIdResponse{
		SignId: sign_id,
	}
	return response, nil
}

// Get signs that should be displayed on a kiosk. Streams.
func (s *DisplayServer) GetSignIdsForKioskId(*pb.GetSignIdForKioskIdRequest, pb.Display_GetSignIdsForKioskIdServer) error {
	return nil
}
