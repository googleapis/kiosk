/*
 Copyright 2018 Google LLC. All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package test

import (
	"context"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/golang/protobuf/ptypes/empty"
	pb "github.com/googleapis/kiosk/generated"
	"google.golang.org/grpc"
)

func address() string {
	host := os.Getenv("KIOSK_SERVER")
	if host == "" {
		host = "localhost"
	}
	port := os.Getenv("KIOSK_PORT")
	if port == "" {
		port = "8080"
	}
	address := host + ":" + port
	fmt.Printf("from %s\n", address)
	return address
}

func assertNoError(t *testing.T, err error) {
	if err != nil {
		t.Fatalf("%v", err)
	}
}

func assertEqual(t *testing.T, a interface{}, b interface{}) {
	if a != b {
		t.Errorf("%s != %s", a, b)
	}
}

func TestKiosk(t *testing.T) {
	// Create a connection to the server.
	ctx, _ := context.WithTimeout(context.TODO(), 1*time.Second)
	conn, err := grpc.DialContext(ctx, address(), grpc.WithInsecure(), grpc.WithBlock())
	assertNoError(t, err)
	defer conn.Close()

	// Create a client for the connection.
	c := pb.NewDisplayClient(conn)

	// Delete all kiosks.
	{
		response, err := c.ListKiosks(ctx, &empty.Empty{})
		assertNoError(t, err)
		for _, k := range response.Kiosks {
			_, err := c.DeleteKiosk(ctx, &pb.DeleteKioskRequest{Id: int32(k.Id)})
			assertNoError(t, err)
		}
	}
	// List all kiosks and verify that the count is zero.
	{
		response, err := c.ListKiosks(ctx, &empty.Empty{})
		assertNoError(t, err)
		assertEqual(t, len(response.Kiosks), 0)
	}
	// Delete all signs.
	{
		response, err := c.ListSigns(ctx, &empty.Empty{})
		assertNoError(t, err)
		for _, s := range response.Signs {
			_, err := c.DeleteSign(ctx, &pb.DeleteSignRequest{Id: int32(s.Id)})
			assertNoError(t, err)
		}
	}
	// List all signs and verify that the count is zero.
	{
		response, err := c.ListSigns(ctx, &empty.Empty{})
		assertNoError(t, err)
		assertEqual(t, len(response.Signs), 0)
	}
	// Create a kiosk.
	var kiosk_id int32
	{
		kiosk := &pb.Kiosk{
			Name: "foo",
		}
		newkiosk, err := c.CreateKiosk(ctx, kiosk)
		assertNoError(t, err)
		kiosk_id = newkiosk.Id
	}
	// Create a sign.
	var sign1_id int32
	{
		sign := &pb.Sign{
			Name: "A",
		}
		newsign, err := c.CreateSign(ctx, sign)
		assertNoError(t, err)
		sign1_id = newsign.Id
	}
	// Create a second sign.
	var sign2_id int32
	{
		sign := &pb.Sign{
			Name: "B",
		}
		newsign, err := c.CreateSign(ctx, sign)
		assertNoError(t, err)
		sign2_id = newsign.Id
	}
	// List all kiosks and verify the count.
	{
		response, err := c.ListKiosks(ctx, &empty.Empty{})
		assertNoError(t, err)
		assertEqual(t, len(response.Kiosks), 1)
	}
	// List all signs and verify the count.
	{
		response, err := c.ListSigns(ctx, &empty.Empty{})
		assertNoError(t, err)
		assertEqual(t, len(response.Signs), 2)
	}
	// Set the sign for a kiosk.
	{
		_, err := c.SetSignIdForKioskIds(ctx, &pb.SetSignIdForKioskIdsRequest{
			SignId:   int32(sign1_id),
			KioskIds: []int32{int32(kiosk_id)},
		})
		assertNoError(t, err)
	}
	// Get the sign id for a kiosk id.
	{
		response, err := c.GetSignIdForKioskId(ctx, &pb.GetSignIdForKioskIdRequest{
			KioskId: int32(kiosk_id),
		})
		assertNoError(t, err)
		assertEqual(t, response.SignId, sign1_id)
	}
	// Set the sign for a kiosk.
	{
		_, err := c.SetSignIdForKioskIds(ctx, &pb.SetSignIdForKioskIdsRequest{
			SignId:   int32(sign2_id),
			KioskIds: []int32{int32(kiosk_id)},
		})
		assertNoError(t, err)
	}
	// Get the sign id for a kiosk id.
	{
		response, err := c.GetSignIdForKioskId(ctx, &pb.GetSignIdForKioskIdRequest{
			KioskId: int32(kiosk_id),
		})
		assertNoError(t, err)
		assertEqual(t, response.SignId, sign2_id)
	}
	// Delete all kiosks.
	{
		response, err := c.ListKiosks(ctx, &empty.Empty{})
		assertNoError(t, err)
		for _, k := range response.Kiosks {
			_, err := c.DeleteKiosk(ctx, &pb.DeleteKioskRequest{Id: int32(k.Id)})
			assertNoError(t, err)
		}
	}
	// Delete all signs.
	{
		response, err := c.ListSigns(ctx, &empty.Empty{})
		assertNoError(t, err)
		for _, s := range response.Signs {
			_, err := c.DeleteSign(ctx, &pb.DeleteSignRequest{Id: int32(s.Id)})
			assertNoError(t, err)
		}
	}
}
