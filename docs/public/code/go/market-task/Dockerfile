FROM golang:latest

# WORKDIR $GOPATH/src/zq-task/task/
# COPY . $GOPATH/src/zq-task/task/

WORKDIR /task/
COPY . /task/

RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
# RUN go get -u github.com/golang/dep/cmd/dep
# RUN dep ensure
ENV GO111MODULE=on GOPROXY=https://goproxy.io,direct

RUN go build -o market-task .

# EXPOSE 8000
ENTRYPOINT ["./market-task"]
