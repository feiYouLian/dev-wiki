package main

import (
	"log"

	"market.task/runner"
)

func main() {
	hubconfig, err := runner.InitHubConfig("")
	if err != nil {
		log.Panic(err)
	}
	hub := runner.NewHub(hubconfig)
	hub.AddRunner(runner.NewRunner("order_auto_cancel", runner.CancelOrderCheck, runner.CancelOrderRun))
	hub.Start()
}
