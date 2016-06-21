#!/bin/bash
set -e
set -x
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$ROOT"

cd server/grpc
protoc --go_out=plugins=grpc:../ */*.proto
cd ..

CGO_ENABLED=0 GOOS=linux go build -a -tags netgo -ldflags '-w' -o ./build/gopic .

cd ..

docker build --no-cache -t  avezila/gopic-server    -f deploy/server/go.Dockerfile . 
docker build --no-cache -t  avezila/gopic-server-mongo -f deploy/server/mongo.Dockerfile .
