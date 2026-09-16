package exmples

import (
	"fmt"
	"time"
)

// Producer Producer
func Producer(factor int, out chan<- int) {
	for i := 1; ; i++ {
		time.Sleep(time.Second)
		out <- i * factor
	}
}

// Consumer Consumer
func Consumer(in <-chan int) {
	for v := range in {
		fmt.Println(v)
	}
}

// Pc Pc
func Pc() {
	ch := make(chan int, 64) // 成果队列

	go Producer(3, ch) // 生成 3 的倍数的序列
	go Producer(5, ch) // 生成 5 的倍数的序列
	go Consumer(ch)    // 消费 生成的队列

	// 运行一定时间后退出
	time.Sleep(5 * time.Second)
}
