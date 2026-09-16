package exmples

import (
	"fmt"
	"os"
	"os/signal"
	"strings"
	"sync"
	"syscall"
	"time"
)

type (
	subscriber   chan interface{}
	topicHandler func(interface{}) bool
)

// Publisher Publisher
type Publisher struct {
	m           sync.RWMutex
	buffer      int
	timeout     time.Duration
	subscribers map[subscriber]topicHandler
}

// NewPublisher NewPublisher
func NewPublisher(buffer int, timeout time.Duration) *Publisher {
	return &Publisher{
		buffer:      buffer,
		timeout:     timeout,
		subscribers: make(map[subscriber]topicHandler, 0),
	}
}

// Subscribe Subscribe
func (p *Publisher) Subscribe() chan interface{} {
	return p.SubscribeTopic(nil)
}

// SubscribeTopic SubscribeTopic
func (p *Publisher) SubscribeTopic(topic topicHandler) chan interface{} {
	p.m.Lock()
	defer p.m.Unlock()
	sc := make(chan interface{}, p.buffer)
	p.subscribers[sc] = topic
	return sc
}

// UnSubscribe UnSubscribe
func (p *Publisher) UnSubscribe(sc chan interface{}) {
	p.m.Lock()
	defer p.m.Unlock()
	delete(p.subscribers, sc)
	close(sc)
}

// Close Close
func (p *Publisher) Close() {
	p.m.Lock()
	defer p.m.Unlock()
	for sc := range p.subscribers {
		delete(p.subscribers, sc)
		close(sc)
	}
}

// Publish Publish
func (p *Publisher) Publish(v interface{}) {
	p.m.RLock()
	defer p.m.RUnlock()

	var wg sync.WaitGroup
	for sc, topic := range p.subscribers {
		wg.Add(1)
		go p.sendTopic(&wg, sc, topic, v)
	}
	wg.Wait()
}

func (p *Publisher) sendTopic(wg *sync.WaitGroup, sc subscriber, topic topicHandler, v interface{}) {
	defer wg.Done()

	if topic != nil && !topic(v) {
		return
	}
	select {
	case sc <- v:
	case <-time.After(p.timeout):
	}
}

// Pubsub Pubsub
func Pubsub() {
	p := NewPublisher(10, 100*time.Millisecond)
	defer p.Close()

	all := p.Subscribe()
	golang := p.SubscribeTopic(func(v interface{}) bool {
		if s, ok := v.(string); ok {
			return strings.Contains(s, "golang")
		}
		return false
	})

	go func() {
		for msg := range all {
			time.Sleep(100 * time.Millisecond)
			fmt.Println("all:", msg)
		}
	}()

	go func() {
		for msg := range golang {
			time.Sleep(100 * time.Millisecond)
			fmt.Println("golang:", msg)
		}
	}()

	for i := 0; i < 5; i++ {
		p.Publish("hello,  world!")
		p.Publish("hello, golang!")
	}

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGINT, syscall.SIGTERM)
	// signal.Notify(sig, os.Interrupt)
	fmt.Printf("quit %v \n", <-sig)
}
