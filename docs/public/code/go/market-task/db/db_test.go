package db

import (
	"fmt"
	"log"
	"testing"

	_ "github.com/go-sql-driver/mysql"
)

func TestInitRedis(t *testing.T) {
	r := InitRedis("192.168.0.148", 6379, "123456")

	sub := r.Subscribe("test")
	exit := make(chan byte, 0)
	send := 10 * 1000

	go func() {
		i := 0
		for {
			select {
			case msg := <-sub.Channel():
				i++
				if i == send {
					log.Println(msg)
					exit <- 1
				}
			}
		}
	}()

	for index := 1; index <= send; index++ {
		r.Publish("test", fmt.Sprintf("hello %v", index))
	}
	<-exit
}
