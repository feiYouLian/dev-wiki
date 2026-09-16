package exmples

import (
	"fmt"
	"time"
)

// ControlNum ControlNum
func ControlNum(max int) {
	todo := make(chan func(interface{}), 10)
	go func() {
		for i := 0; i < 10; i++ {
			todo <- func(thing interface{}) { time.Sleep(3 * time.Second); fmt.Println("do", thing) }
		}
	}()
	limit := make(chan int, max)
	i := 0
	for do := range todo {
		go func(thing int) {
			fmt.Println(thing)
			limit <- 1
			do(thing)
			<-limit
		}(i)
		i++
	}
}
