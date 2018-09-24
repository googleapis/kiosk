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

// DisplayServer manages a collection of kiosks.
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

// NewDisplayServer creates and returns a new DisplayServer.
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

// CreateKiosk creates and enrolls a kiosk for sign display.
func (s *DisplayServer) CreateKiosk(c context.Context, kiosk *pb.Kiosk) (*pb.Kiosk, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	kiosk.Id = s.nextKioskId
	s.kiosks[kiosk.Id] = kiosk
	s.nextKioskId++
	return kiosk, nil
}

// ListKiosks returns a list of active kiosks.
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

// GetKiosk returns a kiosk whose ID is r.Id.
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

// DeleteKiosk deletes the kiosk with ID r.Id.
func (s *DisplayServer) DeleteKiosk(c context.Context, r *pb.DeleteKioskRequest) (*google_protobuf.Empty, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.kiosks[i] != nil {
		delete(s.kiosks, i)
		if len(s.kiosks) == 0 {
			s.nextKioskId = 1
		}
		return &google_protobuf.Empty{}, nil
	} else {
		return nil, errors.New("invalid kiosk id")
	}
}

// CreateSign creates and enrolls a sign for sign display.
func (s *DisplayServer) CreateSign(c context.Context, sign *pb.Sign) (*pb.Sign, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	sign.Id = s.nextSignId
	s.signs[sign.Id] = sign
	s.nextSignId++
	return sign, nil
}

// ListSigns returns a list of active signs.
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

// GetSign returns the sign with ID r.Id.
func (s *DisplayServer) GetSign(c context.Context, r *pb.GetSignRequest) (*pb.Sign, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.signs[i] != nil {
		return s.signs[i], nil
	}
	return nil, errors.New("invalid sign id")
}

// DeleteSign deletes the sign with ID r.Id.
func (s *DisplayServer) DeleteSign(c context.Context, r *pb.DeleteSignRequest) (*google_protobuf.Empty, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	i := r.Id
	if s.signs[i] != nil {
		delete(s.signs, i)
		if len(s.signs) == 0 {
			s.nextSignId = 1
		}
		return &google_protobuf.Empty{}, nil
	}
	return nil, errors.New("invalid sign id")
}

// SetSignIdForKioskIds sets a sign for display on one or more kiosks.
func (s *DisplayServer) SetSignIdForKioskIds(c context.Context, r *pb.SetSignIdForKioskIdsRequest) (*google_protobuf.Empty, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	for _, kioskID := range r.KioskIds {
		s.signIdsForKioskIds[kioskID] = r.SignId
		if s.subscribers[kioskID] != nil {
			for c := range s.subscribers[kioskID] {
				c <- r.SignId
			}
		}
	}
	if len(r.KioskIds) == 0 {
		var kioskID int32
		for kioskID = 1; kioskID < s.nextKioskId; kioskID++ {
			s.signIdsForKioskIds[kioskID] = r.SignId
			if s.subscribers[kioskID] != nil {
				for c := range s.subscribers[kioskID] {
					c <- r.SignId
				}
			}
		}
	}
	return &google_protobuf.Empty{}, nil
}

// GetSignIdForKioskId gets the sign that should be displayed on a kiosk.
func (s *DisplayServer) GetSignIdForKioskId(c context.Context, r *pb.GetSignIdForKioskIdRequest) (*pb.GetSignIdResponse, error) {
	s.mux.Lock()
	defer s.mux.Unlock()
	kioskID := r.KioskId
	if s.kiosks[kioskID] == nil {
		return nil, errors.New("invalid kiosk id")
	}
	signID := s.signIdsForKioskIds[kioskID]
	response := &pb.GetSignIdResponse{
		SignId: signID,
	}
	return response, nil
}

// GetSignIdsForKioskId gets the signs that should be displayed on a kiosk. Streams.
func (s *DisplayServer) GetSignIdsForKioskId(r *pb.GetSignIdForKioskIdRequest, stream pb.Display_GetSignIdsForKioskIdServer) error {
	s.mux.Lock()
	defer s.mux.Unlock()
	kioskID := r.KioskId
	if s.kiosks[kioskID] == nil {
		return errors.New("invalid kiosk id")
	}
	signID := s.signIdsForKioskIds[kioskID]
	response := &pb.GetSignIdResponse{
		SignId: signID,
	}
	stream.Send(response)
	ch := make(chan int32)
	if s.subscribers[kioskID] == nil {
		s.subscribers[kioskID] = make(map[chan int32]bool)
	}
	s.subscribers[kioskID][ch] = true
	s.mux.Unlock() // unlock to wait for sign updates
	timer := time.NewTimer(s.SessionLifetime)
	running := true
	for running {
		select {
		case <-timer.C:
			running = false
			break
		case signID, ok := <-ch:
			response := &pb.GetSignIdResponse{
				SignId: signID,
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
	fmt.Printf("removing subscriber for kiosk %d\n", kioskID)
	delete(s.subscribers[kioskID], ch)
	return nil
}
