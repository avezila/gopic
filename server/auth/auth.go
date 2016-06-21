package auth

import (
	"fmt"

	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

type Auth struct {
}

func New(server *grpc.Server) (*Auth, error) {
	auth := &Auth{}
	RegisterAuthServer(server, auth)
	return auth, nil
}

func (s *Auth) Register(ctx context.Context, req *RegisterReq) (*RegisterRes, error) {
	fmt.Println(req)
	return &RegisterRes{}, nil
}
