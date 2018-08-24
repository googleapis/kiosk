// Copyright 2018 Google LLC. All Rights Reserved.
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
	"fmt"
	"sync"
	"time"

	google_protobuf "github.com/golang/protobuf/ptypes/empty"
	pb "github.com/googleapis/kiosk/generated"
	context "golang.org/x/net/context"
)

type DisplayServer struct {
	SessionLifetime    time.Duration
	kiosks             map[int32]*pb.Kiosk
	signs              map[int32]*pb.Sign
	signIdsForKioskIds map[int32]int32
	subscribers        map[int32]map[chan int32]bool
	nextKioskId        int32
	nextSignId         int32
	mux                sync.Mutex
}

func NewDisplayServer() *DisplayServer {
	return &DisplayServer{
		SessionLifetime:    24 * time.Hour,
		kiosks:             make(map[int32]*pb.Kiosk),
		signs:              make(map[int32]*pb.Sign),
		signIdsForKioskIds: make(map[int32]int32),
		subscribers:        make(map[int32]map[chan int32]bool),
		nextKioskId:        1,
		nextSignId:         1,
	}
}

// Create a kiosk. This enrolls the kiosk for sign display.
func (s *DisplayServer) CreateKiosk(c context.Context, kiosk *pb.Kiosk) (*pb.Kiosk, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	kiosk.Id = s.nextKioskId
	s.kiosks[kiosk.Id] = kiosk
	s.nextKioskId++
	return kiosk, nil
}

// List active kiosks.
func (s *DisplayServer) ListKiosks(c context.Context, x *google_protobuf.Empty) (*pb.ListKiosksResponse, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
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
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.kiosks[i] != nil {
		return s.kiosks[i], nil
	} else {
		return nil, errors.New("invalid kiosk id")
	}
}

// Delete a kiosk.
func (s *DisplayServer) DeleteKiosk(c context.Context, r *pb.DeleteKioskRequest) (*google_protobuf.Empty, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.kiosks[i] != nil {
		delete(s.kiosks, i)
		s.kiosks[i] = nil
		return &google_protobuf.Empty{}, nil
	} else {
		return nil, errors.New("invalid kiosk id")
	}
}

// Create a sign. This enrolls the sign for sign display.
func (s *DisplayServer) CreateSign(c context.Context, sign *pb.Sign) (*pb.Sign, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	sign.Id = s.nextSignId
	s.signs[sign.Id] = sign
	s.nextSignId++
	return sign, nil
}

// List active signs.
func (s *DisplayServer) ListSigns(c context.Context, x *google_protobuf.Empty) (*pb.ListSignsResponse, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
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
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.signs[i] != nil {
		return s.signs[i], nil
	} else {
		return nil, errors.New("invalid sign id")
	}
}

// Delete a sign.
func (s *DisplayServer) DeleteSign(c context.Context, r *pb.DeleteSignRequest) (*google_protobuf.Empty, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.signs[i] != nil {
		delete(s.signs, i)
		return &google_protobuf.Empty{}, nil
	} else {
		return nil, errors.New("invalid sign id")
	}
}

// Set a sign for display on one or more kiosks.
func (s *DisplayServer) SetSignIdForKioskIds(c context.Context, r *pb.SetSignIdForKioskIdsRequest) (*google_protobuf.Empty, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	for _, kiosk_id := range r.KioskIds {
		s.signIdsForKioskIds[kiosk_id] = r.SignId
		if s.subscribers[kiosk_id] != nil {
			for c, _ := range s.subscribers[kiosk_id] {
				c <- r.SignId
			}
		}
	}
	if len(r.KioskIds) == 0 {
		var kiosk_id int32
		for kiosk_id = 1; kiosk_id < s.nextKioskId; kiosk_id++ {
			s.signIdsForKioskIds[kiosk_id] = r.SignId
			if s.subscribers[kiosk_id] != nil {
				for c, _ := range s.subscribers[kiosk_id] {
					c <- r.SignId
				}
			}
		}
	}
	return &google_protobuf.Empty{}, nil
}

// Get the sign that should be displayed on a kiosk.
func (s *DisplayServer) GetSignIdForKioskId(c context.Context, r *pb.GetSignIdForKioskIdRequest) (*pb.GetSignIdResponse, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	kiosk_id := r.KioskId
	if s.kiosks[kiosk_id] == nil {
		return nil, errors.New("invalid kiosk id")
	}
	sign_id := s.signIdsForKioskIds[kiosk_id]
	response := &pb.GetSignIdResponse{
		SignId: sign_id,
	}
	return response, nil
}

// Get signs that should be displayed on a kiosk. Streams.
func (s *DisplayServer) GetSignIdsForKioskId(r *pb.GetSignIdForKioskIdRequest, stream pb.Display_GetSignIdsForKioskIdServer) error {
	s.mux.Lock()
	defer s.mux.Unlock()
	kiosk_id := r.KioskId
	if s.kiosks[kiosk_id] == nil {
		return errors.New("invalid kiosk id")
	}
	sign_id := s.signIdsForKioskIds[kiosk_id]
	response := &pb.GetSignIdResponse{
		SignId: sign_id,
	}
	stream.Send(response)
	ch := make(chan int32)
	if s.subscribers[kiosk_id] == nil {
		s.subscribers[kiosk_id] = make(map[chan int32]bool)
	}
	s.subscribers[kiosk_id][ch] = true
	s.mux.Unlock() // unlock to wait for sign updates
	timer := time.NewTimer(s.SessionLifetime)
	running := true
	for running {
		select {
		case <-timer.C:
			running = false
			break
		case sign_id, ok := <-ch:
			response := &pb.GetSignIdResponse{
				SignId: sign_id,
			}
			err := stream.Send(response)
			if !ok || err != nil {
				fmt.Printf("error %+v\n", err)
				running = false
				break
			}
		}
	}
	s.mux.Lock() // relock to unsubscribe
	fmt.Printf("removing subscriber for kiosk %d\n", kiosk_id)
	delete(s.subscribers[kiosk_id], ch)
	return nil
}
