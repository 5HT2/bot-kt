# scripts
Just an easy way to reboot the bot if it isn't running, eg if your system restarted or you used the `;shutdown` command.

If you want to use this, modify it to your path, then run
```
mkdir -p ~/.config/systemd/user
cp bot-kt.timer ~/.config/systemd/user
cp bot-kt.service ~/.config/systemd/user
systemctl enable --now bot-kt.timer # enables and starts process
```

If you have `bot-kt` in your home folder then you can just run `setup.sh`
