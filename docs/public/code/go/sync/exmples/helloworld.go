package exmples

import (
	"fmt"
	"time"
)

// HelloWorld HelloWorld
func HelloWorld() {
	done := make(chan int, 1)
	go func() {
		time.Sleep(3 * time.Second)
		fmt.Println("hello world")
		done <- 1
	}()
	for {
		select {
		case <-done:
			fmt.Println("ending")
			return
		case <-time.After(time.Second):
			fmt.Println("waiting...")
		}
	}
}
