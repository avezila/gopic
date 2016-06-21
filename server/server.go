package main

import (
	"net"

	"google.golang.org/grpc"
)

type Server struct {
	listener net.Listener
	grpc     *grpc.Server
}

func NewServer(port string) (s *Server, err error) {
	s = &Server{}
	s.listener, err = net.Listen("tcp", port)
	if err != nil {
		return nil, err
	}
	s.grpc = grpc.NewServer()
	return s, nil
}

func (s *Server) Serve() {
	s.grpc.Serve(s.listener)
}
