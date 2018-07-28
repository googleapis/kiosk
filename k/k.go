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

// The k tool provides a command line interface to the Kiosk API.
package main

import (
	"context"
	"flag"
	"fmt"
	"log"

	"crypto/tls"

	"github.com/docopt/docopt-go"
	"github.com/golang/protobuf/ptypes/empty"
	pb "github.com/googleapis/kiosk/generated"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

const (
	useSSL         = false
	host           = "localhost"
	defaultMessage = "Hello."
)

func main() {
	usage := `Kiosk Tool.

  Usage:
    k create kiosk <name>
    k list kiosks
    k get kiosk <id>
    k delete kiosk <id>
    k create sign <name>
    k list signs
    k get sign <id>
    k delete sign <id>
    k set sign <id> for kiosk <id>
    k set sign <id> for all kiosks
    k get sign for kiosk <id>
    k get signs for kiosk <id>

  Options:
    --name=<name> Name for new kiosk or sign.
`
	arguments, _ := docopt.ParseDoc(usage)

	fmt.Println(arguments)

	flag.Parse()

	// Set up a connection to the server.
	var conn *grpc.ClientConn
	var err error
	if !useSSL {
		conn, err = grpc.Dial("localhost:8080", grpc.WithInsecure())
	} else {
		conn, err = grpc.Dial("localhost:443",
			grpc.WithTransportCredentials(credentials.NewTLS(&tls.Config{
				// remove the following line if the server certificate is signed by a certificate authority
				InsecureSkipVerify: true,
			})))
	}

	if err != nil {
		log.Fatalf("did not connect: %v", err)
	}
	defer conn.Close()
	c := pb.NewDisplayClient(conn)
	fmt.Printf("connected %+v\n", c)

	ctx := context.TODO()

	if arguments["create"].(bool) && arguments["kiosk"].(bool) {
		kiosk := &pb.Kiosk{
			Name: arguments["<name>"].(string),
		}
		newkiosk, err := c.CreateKiosk(ctx, kiosk)
		fmt.Printf("created %+v (err %+v)\n", newkiosk, err)
	}

	if arguments["list"].(bool) && arguments["kiosks"].(bool) {
		response, err := c.ListKiosks(ctx, &empty.Empty{})
		fmt.Printf("listing %+v (err %+v)\n", response, err)
	}

	if arguments["get"].(bool) && arguments["kiosk"].(bool) {
		id, _ := arguments.Int("<id>")

		kiosk, err := c.GetKiosk(ctx, &pb.GetKioskRequest{Id: int32(id)})
		fmt.Printf("created %+v (err %+v)\n", kiosk, err)
	}

	if arguments["delete"].(bool) && arguments["kiosk"].(bool) {
		kiosk, err := c.DeleteKiosk(ctx, &pb.DeleteKioskRequest{Id: arguments["<id>"].(int32)})
		fmt.Printf("created %+v (err %+v)\n", kiosk, err)
	}

	//	func (c *displayClient) SetSignForKiosks(ctx context.Context, in *SetSignRequest, opts ...grpc.CallOption) (*google_protobuf.Empty, error) {
	//	func (c *displayClient) GetSignForKiosk(ctx context.Context, in *GetSignForKioskRequest, opts ...grpc.CallOption) (*Sign, error) {
	//	func (c *displayClient) GetSignsForKiosk(ctx context.Context, in *GetSignForKioskRequest, opts ...grpc.CallOption) (Display_GetSignsForKioskClient, error) {

}
