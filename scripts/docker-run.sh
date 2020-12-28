#!/bin/bash
docker run -d --restart=always --name bot-kt -v $(pwd)/config:/bot/config bot-kt:latest
