# 卸载

> yum remove docker docker-common docker-selinux docker-engine docker-io

# step 1: 安装必要的一些系统工具

> sudo yum install -y yum-utils device-mapper-persistent-data lvm2

# Step 2: 添加软件源信息

> sudo yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

# Step 3: 更新 yum 软件源缓存，并安装 docker-ce。

> sudo yum makecache fast

> sudo yum -y install docker-ce

问题

```sh
package docker-ce-3:19.03.2-3.el7.x86_64 requires containerd.io >= 1.2.2-3, but none of the providers can be installed
- cannot install the best candidate for the job
- package containerd.io-1.2.2-3.3.el7.x86_64 is excluded
- package containerd.io-1.2.2-3.el7.x86_64 is excluded
- package containerd.io-1.2.4-3.1.el7.x86_64 is excluded
- package containerd.io-1.2.5-3.1.el7.x86_64 is excluded
- package containerd.io-1.2.6-3.3.el7.x86_64 is excluded
(try to add '--skip-broken' to skip uninstallable packages or '--nobest' to use not only best candidate packages)
```

解决

1. 查看所有仓库中所有 docker 版本，并选择特定版本安装

   > yum list docker-ce --showduplicates | sort -r

2. 安装低版本的软件包
   > yum -y install docker-ce-18.06.0.ce-3.el7

# Step 4: 开启 Docker 服务

启动:

> systemctl start docker

开机启动：

> systemctl enable docker

# Step 5: 配置镜像库

vim /etc/docker/daemon.json

```json
{ "registry-mirrors": ["https://registry.docker-cn.com"] }
```

重启 （配置生效）

> sudo systemctl daemon-reload  
> sudo systemctl restart docker

---

firewalld 的没有信任 docker 的 ip 地址，将所有 docker 的 ip 添加到信任区域。

> firewall-cmd --zone=trusted --add-source=172.19.0.1/16 --permanent
> firewall-cmd --zone=trusted --add-source=172.17.0.1/16 --permanent
> firewall-cmd --zone=trusted --add-source=127.0.0.1/8 --permanent
> firewall-cmd --reload //重启防火墙（一般也要重启 sudo systemctl restart docker, service docker restart）

reason: getaddrinfo EAI_AGAIN？

> docker build -t web . `--network=host` (重启实例)

---

> docker run -d -p 3306:3306 --name mysql -v /opt/mydata/:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 mysql:latest

> docker exec -it mysql /bin/bash

> select host,user,plugin,authentication_string from mysql.user;

> ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
> flush privileges;

> systemctl status firewalld

> systemctl start firewalld

> firewall-cmd --permanent --zone=public --add-port=3306/tcp

//添加安全组规则

> systemctl restart docker

> docker start mysql

> docker inspect mysql //配置 Gateway

> docker run -id --privileged=true --name=nexus3 --restart=always -p 3333:3333 -v /opt/nexus3/nexus-data:/var/nexus-data sonatype/nexus3

> firewall-cmd --zone=public --add-port=3333/tcp --permanent

---

1.启动防火墙

> systemctl start firewalld

2.禁用防火墙

> systemctl stop firewalld

3.设置开机启动

> systemctl enable firewalld

4.停止并禁用开机启动

> sytemctl disable firewalld

5.重启防火墙

> firewall-cmd --reload

6.查看状态

> systemctl status firewalld 或者 firewall-cmd --state

14.将接口添加到区域(默认接口都在 public)

> firewall-cmd --zone=public --add-interface=eth0(永久生效再加上 --permanent 然后 reload 防火墙)

15.设置默认接口区域

> firewall-cmd --set-default-zone=public(立即生效，无需重启)

17.查看指定区域所有打开的端口

> firewall-cmd --zone=public --list-ports

18.查看防火墙 信任的域

> firewall-cmd --zone=trusted --list-sources

18.在指定区域打开端口（记得重启防火墙）

> firewall-cmd --zone=public --add-port=80/tcp(永久生效再加上 --permanent)

export SPARK_HISTORY_OPTS="-Dspark.history.ui.port=18080 -Dspark.history.retainedApplications=3 -Dspark.history.fs.logDirectory=hdfs://master:9000/spark-logs"
