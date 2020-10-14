#!/bin/bash
docker run -d --rm --name bot-kt -v $(pwd)/config:/bot/config bot-kt:latest
