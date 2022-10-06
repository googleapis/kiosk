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

// Run this tool in the "images" directory to preload a kiosk server with signs.

package main

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"time"

	"github.com/golang/protobuf/ptypes/empty"
	pb "github.com/googleapis/kiosk/rpc"
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

func create_sign(ctx context.Context, c pb.DisplayClient, name string, path string) {
	b, err := ioutil.ReadFile(path)
	if err != nil {
		panic(err)
	}
	sign := &pb.Sign{
		Name:  name,
		Text:  name,
		Image: b,
	}
	fmt.Printf("creating sign %s\n", sign.Name)
	_, err = c.CreateSign(ctx, sign)
	if err != nil {
		panic(err)
	}
}

func main() {
	// Create a connection to the server.
	ctx, _ := context.WithTimeout(context.TODO(), 1*time.Second)
	conn, err := grpc.DialContext(ctx, address(), grpc.WithInsecure(), grpc.WithBlock())
	if err != nil {
		panic(err)
	}
	defer conn.Close()

	// Create a client for the connection.
	c := pb.NewDisplayClient(conn)

	// Delete all signs.
	{
		response, err := c.ListSigns(ctx, &empty.Empty{})
		if err != nil {
			panic(err)
		}
		for _, s := range response.Signs {
			fmt.Printf("deleting sign %d\n", s.Id)
			_, err := c.DeleteSign(ctx, &pb.DeleteSignRequest{Id: int32(s.Id)})
			if err != nil {
				panic(err)
			}
		}
	}
	// Create signs for each sample image.
	{
		create_sign(ctx, c, "cat", "adorable-animal-cat-20787.jpg")
		create_sign(ctx, c, "gorilla", "animal-animal-photography-black-35992.jpg")
		create_sign(ctx, c, "peacock", "animal-avian-beak-326900.jpg")
		create_sign(ctx, c, "dog", "animal-canine-close-up-733416.jpg")
		create_sign(ctx, c, "butterfly", "beautiful-bloom-blossom-326067.jpg")
	}
}
