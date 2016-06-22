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

func (s *Auth) Login(ctx context.Context, req *LoginReq) (res *LoginRes, err error) {
	email := req.Login
	errors := []Error{}
	defer func() {
		if err != nil {
			errors = append(errors, Error_INTERNAL)
		}
		if len(errors) != 0 {
			res = &LoginRes{Error: errors}
		}
	}()
	req.Login = strings.ToLower(req.Login)
	email = strings.ToLower(email)

	isLogin := len(req.Login) >= 1
	isEmail := govalidator.IsEmail(email)
	if !isEmail && !isLogin {
		errors = append(errors, Error_BAD_LOGIN)
		return
	}
	row := &struct {
		Id       string
		Password []byte
		Login    string
	}{}

	err = nil
	if isEmail {
		err = s.userDb.Find(bson.M{"email": email}).One(row)
	}
	if !isEmail || (err != nil && isLogin) {
		err = s.userDb.Find(bson.M{"login": req.Login}).One(row)
	}
	if err != nil {
		errors = append(errors, Error_LOGIN_IS_NOT_EXISTS)
		err = nil
		return
	}
	if bcrypt.CompareHashAndPassword(row.Password,
		[]byte(row.Login+req.Password)) != nil {
		errors = append(errors, Error_BAD_PASSWORD)
		return
	}

	session, err := GenerateRandomString(32)
	if err != nil {
		return
	}

	err = s.userDb.Update(bson.M{"id": row.Id}, bson.M{
		"$push": bson.M{"session": session},
	})
	if err != nil {
		return
	}
	return &LoginRes{Session: session}, nil
}

func (s *Auth) CheckSession(ctx context.Context,
	req *CheckSessionReq) (res *CheckSessionRes, err error) {
	errors := []Error{}
	defer func() {
		if err != nil {
			errors = append(errors, Error_INTERNAL)
		}
		if len(errors) != 0 {
			res = &CheckSessionRes{Error: errors}
		}
	}()
	n, err := s.userDb.Find(bson.M{
		"session": bson.M{"$in": []string{req.Session}}}).Count()
	if err != nil {
		return
	} else if n == 0 {
		errors = append(errors, Error_BAD_SESSION)
		return
	}
	return &CheckSessionRes{}, nil
}

func (s *Auth) Logout(ctx context.Context,
	req *LogoutReq) (res *LogoutRes, err error) {
	errors := []Error{}
	defer func() {
		if err != nil {
			errors = append(errors, Error_INTERNAL)
		}
		if len(errors) != 0 {
			res = &LogoutRes{Error: errors}
		}
	}()
	err = s.userDb.Update(bson.M{"session": bson.M{"$in": []string{req.Session}}},
		bson.M{"$pull": bson.M{"session": req.Session}})
	if err != nil {
		errors = append(errors, Error_BAD_SESSION)
		err = nil
		return
	}
	return &LogoutRes{}, nil
}

func (s *Auth) UserInfo(ctx context.Context,
	req *UserInfoReq) (res *UserInfoRes, err error) {
	errors := []Error{}
	defer func() {
		if err != nil {
			errors = append(errors, Error_INTERNAL)
		}
		if len(errors) != 0 {
			res = &UserInfoRes{Error: errors}
		}
	}()
	user := &struct {
		Login   string
		Email   string
		Session []string
		Id      string
	}{}
	err = s.userDb.Find(bson.M{
		"session": bson.M{"$in": []string{req.Session}}}).One(user)
	if err != nil {
		errors = append(errors, Error_BAD_SESSION)
		err = nil
		return
	}

	return &UserInfoRes{
		Email:   user.Email,
		Login:   user.Login,
		Id:      user.Id,
		Session: user.Session,
	}, nil
}
