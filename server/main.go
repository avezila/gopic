package main

import (
	"fmt"
	"log"
	"net"

	"./pinger"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

func main() {

	listener, err := net.Listen("tcp", ":5353")
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	gserver := grpc.NewServer()

	server, err := pinger.New()
	if err != nil {
		panic(err)
	}

	pinger.RegisterPingerServer(gserver, server)

	ret, err := server.Ping(context.Background(), &pinger.Req{"ping"})
	fmt.Println(ret)
	if err != nil {
		panic(err)
	}

	gserver.Serve(listener)
}
