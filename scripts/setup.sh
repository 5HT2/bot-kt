#!/usr/bin/env bash

# Do NOT use this if your path setup is any different
cd ~/bot-kt
wget https://raw.githubusercontent.com/kami-blue/bot-kt/master/scripts/bot-kt.service
wget https://raw.githubusercontent.com/kami-blue/bot-kt/master/scripts/bot-kt.timer
wget https://raw.githubusercontent.com/kami-blue/bot-kt/master/scripts/start.sh
mkdir -p ~/.config/systemd/user
cp bot-kt.timer ~/.config/systemd/user
cp bot-kt.service ~/.config/systemd/user
chmod +x start.sh
sudo systemctl enable --now ~/.config/systemd/user/bot-kt.service
sudo systemctl enable --now ~/.config/systemd/user/bot-kt.timer

rm bot-kt.service
rm bot-kt.timer
