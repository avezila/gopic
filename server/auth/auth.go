package auth

import (
	"regexp"
	"strings"

	"github.com/asaskevich/govalidator"
	"golang.org/x/crypto/bcrypt"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
	"gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"
)

type Auth struct {
	userDb *mgo.Collection
}

func New(server *grpc.Server) (*Auth, error) {
	session, err := mgo.Dial("mongo")
	if err != nil {
		return nil, err
	}
	auth := &Auth{
		userDb: session.DB("gopic").C("user"),
	}
	RegisterAuthServer(server, auth)
	return auth, nil
}

func validateEmail(email string) bool {
	Re := regexp.MustCompile(`^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,4}$`)
	return Re.MatchString(email)
}

func (s *Auth) Register(ctx context.Context, req *RegisterReq) (res *RegisterRes, err error) {
	errors := []Error{}
	defer func() {
		if err != nil {
			errors = append(errors, Error_INTERNAL)
		}
		if len(errors) != 0 {
			res = &RegisterRes{Error: errors}
		}
	}()
	req.Login = strings.ToLower(req.Login)
	req.Email = strings.ToLower(req.Email)

	if len(req.Login) < 1 {
		errors = append(errors, Error_BAD_LOGIN)
	}
	if !govalidator.IsEmail(req.Email) {
		errors = append(errors, Error_BAD_EMAIL)
	}
	if len(errors) != 0 {
		return
	}

	if n, err := s.userDb.Find(bson.M{"login": req.Login}).Count(); err != nil {
		return nil, err
	} else if n != 0 {
		errors = append(errors, Error_LOGIN_EXISTS)
	}
	if n, err := s.userDb.Find(bson.M{"email": req.Email}).Count(); err != nil {
		return nil, err
	} else if n != 0 {
		errors = append(errors, Error_EMAIL_EXISTS)
	}
	if len(errors) != 0 {
		return
	}

	id, err := GenerateRandomString(16)
	if err != nil {
		return
	}
	session, err := GenerateRandomString(32)
	if err != nil {
		return
	}
	password, err := bcrypt.GenerateFromPassword([]byte(req.Login+req.Password), 8)
	if err != nil {
		return
	}
	err = s.userDb.Insert(bson.M{
		"id":       id,
		"session":  []string{session},
		"login":    req.Login,
		"email":    req.Email,
		"password": password,
	})
	if err != nil {
		return
	}
	return &RegisterRes{Session: session}, nil
}
