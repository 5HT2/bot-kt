#!/usr/bin/env bash

process="$(psgrep java)"
currentVersion="$(cat ~/bot-kt/currentVersion)"

# Run if Java is not running
if [[ "$process" ]]; then
  cd ~/bot-kt/
  java -jar bot-kt-${currentVersion}.jar
fi
