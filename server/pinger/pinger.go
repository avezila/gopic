package pinger

import (
	"fmt"

	"google.golang.org/grpc"

	"golang.org/x/net/context"
)

type Pinger struct {
}

func New(server *grpc.Server) (*Pinger, error) {
	pinger := &Pinger{}
	RegisterPingerServer(server, pinger)
	return pinger, nil
}

func (s *Pinger) Ping(ctx context.Context, req *Req) (*Res, error) {
	fmt.Println(req)
	return &Res{"pong"}, nil
}
