# Deploy gopic server:

### install docker like here:
https://docs.docker.com/engine/installation/linux/fedora/

### install docker-compose like here:
https://docs.docker.com/engine/installation/linux/ubuntulinux/

### Seems for fedora this will:
```
sudo dnf update
curl -fsSL https://get.docker.com/ | sh
sudo systemctl start docker
sudo systemctl enable docker
sudo groupadd docker
sudo usermod -aG docker $USER
# relogin here
sudo su -c "curl -L https://github.com/docker/compose/releases/download/1.8.0-rc1/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose"
sudo chmod +x /usr/local/bin/docker-compose
sudo su -c "curl -L https://raw.githubusercontent.com/docker/compose/$(docker-compose version --short)/contrib/completion/bash/docker-compose > /etc/bash_completion.d/docker-compose"

```

And then just exec ./run-server.sh
