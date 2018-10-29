#!/usr/bin/env sh
sudo docker build -f Dockerfile.template --build-arg BASEIMAGE_BUILD=resin/amd64-debian -t agile-core .