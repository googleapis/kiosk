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

// The k tool provides a command line interface to the Kiosk API.
package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"strings"

	"github.com/golang/protobuf/ptypes/empty"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/metadata"

	"github.com/docopt/docopt-go"

	pb "github.com/googleapis/kiosk/generated"
)

const (
	useSSL = false
)

// before printing a sign, truncate its image
func truncate(sign *pb.Sign) {
	if len(sign.Image) > 16 {
		sign.Image = sign.Image[0:16]
	}
}

func Verify(err error) bool {
	if err != nil {
		log.Printf("%+v", err)
		return false
	}
	return true
}

func Match(a map[string]interface{}, command string) bool {
	words := strings.Split(command, " ")
	for _, w := range words {
		switch v := a[w].(type) {
		case bool:
			if v != true {
				return false
			}
		case string:
			continue
		default:
			return false
		}
	}
	fmt.Printf("%s\n", strings.ToUpper(command))
	return true
}

func main() {
	usage := `Kiosk Tool.

  Usage:
    k create kiosk <name>
    k list kiosks
    k get kiosk <kiosk_id>
    k delete kiosk <kiosk_id>
    k create sign <name> [--text=<text>] [--image=<image>]
    k list signs
    k get sign <sign_id>
    k delete sign <sign_id>
    k set sign <sign_id> for kiosk <kiosk_id>
    k set sign <sign_id> for all kiosks
    k get sign for kiosk <kiosk_id>
    k get signs for kiosk <kiosk_id>

  Options:
    <name> Name for new kiosk or sign.
    --text=<text> Text to display on a sign.
    --image=<image> Image (PNG file) to display on a sign.
    
    `
	args, _ := docopt.ParseDoc(usage)

	host := os.Getenv("KIOSK_SERVER")
	if host == "" {
		host = "localhost"
	}
	port := os.Getenv("KIOSK_PORT")
	if port == "" {
		if useSSL {
			port = "8443"
		} else {
			port = "8080"
		}
	}
	address := host + ":" + port
	fmt.Printf("FROM %s ", address)

	// Create context to use for API calls, including any authorization settings in the environment.
	ctx := context.Background()
	apikey := os.Getenv("KIOSK_APIKEY")
	if apikey != "" {
		log.Printf("Using API key: %s", apikey)
		ctx = metadata.NewOutgoingContext(ctx, metadata.Pairs("x-api-key", apikey))
	}
	token := os.Getenv("KIOSK_TOKEN")
	if token != "" {
		log.Printf("Using authentication token: %s", token)
		ctx = metadata.NewOutgoingContext(ctx, metadata.Pairs("Authorization", fmt.Sprintf("Bearer %s", token)))
	}

	// Set up a connection to the server.
	var conn *grpc.ClientConn
	var err error
	if !useSSL {
		conn, err = grpc.Dial(host+":"+port, grpc.WithInsecure())
	} else {
		conn, err = grpc.Dial(host+":"+port,
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

	if Match(args, "set sign <sign_id> for kiosk <kiosk_id>") {
		sign_id, err := args.Int("<sign_id>")
		kiosk_id, err := args.Int("<kiosk_id>")
		response, err := c.SetSignIdForKioskIds(ctx, &pb.SetSignIdForKioskIdsRequest{
			SignId:   int32(sign_id),
			KioskIds: []int32{int32(kiosk_id)},
		})
		if Verify(err) {
			fmt.Printf("%+v\n", response)
		}
	} else if Match(args, "set sign <sign_id> for all kiosks") {
		sign_id, err := args.Int("<sign_id>")
		response, err := c.SetSignIdForKioskIds(ctx, &pb.SetSignIdForKioskIdsRequest{
			SignId: int32(sign_id),
		})
		if Verify(err) {
			fmt.Printf("%+v\n", response)
		}
	} else if Match(args, "get sign for kiosk <kiosk_id>") {
		kiosk_id, err := args.Int("<kiosk_id>")
		response, err := c.GetSignIdForKioskId(ctx, &pb.GetSignIdForKioskIdRequest{
			KioskId: int32(kiosk_id),
		})
		if Verify(err) {
			fmt.Printf("%+v\n", response)
		}
	} else if Match(args, "get signs for kiosk <kiosk_id>") {
		kiosk_id, err := args.Int("<kiosk_id>")
		client, err := c.GetSignIdsForKioskId(ctx, &pb.GetSignIdForKioskIdRequest{
			KioskId: int32(kiosk_id),
		})
		if Verify(err) {
			for {
				response, err := client.Recv()
				if Verify(err) {
					fmt.Printf("%+v\n", response)
				} else {
					break
				}
			}
		}
	} else if Match(args, "create kiosk <name>") {
		kiosk := &pb.Kiosk{
			Name: args["<name>"].(string),
		}
		newkiosk, err := c.CreateKiosk(ctx, kiosk)
		if Verify(err) {
			fmt.Printf("%+v\n", newkiosk)
		}
	} else if Match(args, "list kiosks") {
		response, err := c.ListKiosks(ctx, &empty.Empty{})
		if Verify(err) {
			for _, kiosk := range response.Kiosks {
				fmt.Printf("%+v\n", kiosk)
			}
		}
	} else if Match(args, "get kiosk <kiosk_id>") {
		id, err := args.Int("<kiosk_id>")
		kiosk, err := c.GetKiosk(ctx, &pb.GetKioskRequest{Id: int32(id)})
		if Verify(err) {
			fmt.Printf("%+v\n", kiosk)
		}
	} else if Match(args, "delete kiosk <kiosk_id>") {
		id, err := args.Int("<kiosk_id>")
		_, err = c.DeleteKiosk(ctx, &pb.DeleteKioskRequest{Id: int32(id)})
		if Verify(err) {
			fmt.Printf("deleted\n")
		}
	} else if Match(args, "create sign") {
		sign := &pb.Sign{
			Name: args["<name>"].(string),
		}
		text, has_text := args.String("--text")
		if has_text == nil {
			sign.Text = text
		}
		image_name, has_image := args.String("--image")
		if has_image == nil {
			image, err := ioutil.ReadFile(image_name)
			if Verify(err) {
				sign.Image = []byte(image)
			} else {
				return
			}
		}
		newsign, err := c.CreateSign(ctx, sign)
		if Verify(err) {
			truncate(newsign)
			fmt.Printf("%+v\n", newsign)
		}
	} else if Match(args, "list signs") {
		response, err := c.ListSigns(ctx, &empty.Empty{})
		if Verify(err) {
			for _, sign := range response.Signs {
				truncate(sign)
				fmt.Printf("%+v\n", sign)
			}
		}
	} else if Match(args, "get sign") {
		id, err := args.Int("<sign_id>")
		sign, err := c.GetSign(ctx, &pb.GetSignRequest{Id: int32(id)})
		if Verify(err) {
			truncate(sign)
			fmt.Printf("%+v\n", sign)
		}
	} else if Match(args, "delete sign") {
		id, err := args.Int("<sign_id>")
		_, err = c.DeleteSign(ctx, &pb.DeleteSignRequest{Id: int32(id)})
		if Verify(err) {
			fmt.Printf("deleted\n")
		}
	}
}
