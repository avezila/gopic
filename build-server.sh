#!/bin/bash
set -e
set -x
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$ROOT"

cd server/grpc
regex='(\w+).proto'
for i in $( ls -1 | grep .proto ); do
  if [[ $i =~ $regex ]]; then
    protoc --go_out=plugins=grpc:../${BASH_REMATCH[1]}/ $i
  fi
done
cd "$ROOT"


cd server
CGO_ENABLED=0 GOOS=linux go build -a -tags netgo -ldflags '-w' -o ./build/gopic .
cd "$ROOT"

cp -r server/grpc client/cli
cd client/cli
npm i
cd "$ROOT"


docker build --no-cache -t  avezila/gopic-server    -f deploy/server/go.Dockerfile . 
docker build --no-cache -t  avezila/gopic-server-mongo -f deploy/server/mongo.Dockerfile .
