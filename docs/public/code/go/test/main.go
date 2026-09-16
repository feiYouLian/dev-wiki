package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func spinner(done *bool, delay time.Duration) {
	for !*done {
		for _, r := range `-\|/` {
			fmt.Printf("\r%c", r)
			time.Sleep(delay)
		}
	}
}

func intercept() {
	ch := make(chan os.Signal)
	signal.Notify(ch, syscall.SIGINT, syscall.SIGTERM, syscall.SIGKILL, syscall.SIGQUIT)
	fmt.Println("\nctrl+c 中止任务")
	<-ch
	log.Println("任务已终止")
	os.Exit(0)
	// do things when catch a close signal
}
