#!/usr/bin/env bash

process="$(psgrep java)"
currentVersion="$(cat /home/mika/bot-kt/currentVersion)"

# Run if Java is not running
if [[ "$process" ]]; then
  cd /home/mika/bot-kt/
  java -jar bot-kt-${currentVersion}.jar
fi
