#!/usr/bin/env bash

process="$(pgrep java)"
currentVersion="$(cat ~/bot-kt/currentVersion)"
javaDir="$(which java)"

# Run if Java is not running
if [[ -z "$process" ]]; then
  cd ~/bot-kt/
  exec ${javaDir} -jar bot-kt-${currentVersion}.jar
fi
