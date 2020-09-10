#!/usr/bin/env bash

process="$(pgrep java)"
currentVersion="$(cat ~/bot-kt/currentVersion)"

# Run if Java is not running
if [[ -z "$process" ]]; then
  cd ~/bot-kt/
  exec /home/mika/jdk1.8.0_261/bin/java -jar bot-kt-${currentVersion}.jar
fi
