set -e

cd grpc
protoc --go_out=plugins=grpc:../go/saver saver.proto
cd ..

CGO_ENABLED=0 GOOS=linux go build -a -tags netgo -ldflags '-w' -o ./build/saver-go ./go

docker build --no-cache -t  pioh/saver-go    -f docker/saver-go* . 
docker build --no-cache -t  pioh/saver-mongo -f docker/saver-mongo* .
