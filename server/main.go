package main

import "./pinger"
import "./auth"

func main() {
	server, err := NewServer(":5353")

	if _, err = pinger.New(server.grpc); err != nil {
		panic(err)
	}
	if _, err = auth.New(server.grpc); err != nil {
		panic(err)
	}

	server.Serve()
}
