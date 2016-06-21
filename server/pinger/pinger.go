package pinger

import (
	"fmt"

	"golang.org/x/net/context"
)

type Pinger struct {
}

func New() (*Pinger, error) {
	return &Pinger{}, nil
}

func (s *Pinger) Ping(ctx context.Context, req *Req) (*Res, error) {
	fmt.Println(req)
	return &Res{"pong"}, nil
}
