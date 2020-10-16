#!/bin/bash
git submodule update --init
docker build . -t bot-kt:latest
