package main

import "sync/exmples"

func main() {
	// exmples.HelloWorld()
	// exmples.Pc()
	// exmples.Pubsub()
	// exmples.ControlNum(3)
	go spinner(100 * time.Millisecond)
	go intercept()
	select {}

}

func spinner(delay time.Duration) {
	for {
		for _, r := range `-\|/` {
			fmt.Printf("\r%c", r)
			time.Sleep(delay)
		}
	}
}

func intercept() {
	ch := make(chan os.Signal)
	signal.Notify(ch, syscall.SIGINT, syscall.SIGTERM, syscall.SIGKILL, syscall.SIGQUIT)
	fmt.Println("ctrl+c 终止任务")
	<-ch
	log.Println("任务已终止")
	os.Exit(0)
	// do things when catch a close signal
}
