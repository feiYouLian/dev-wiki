package exmples

import (
	"context"
	"fmt"
	"sync"
	"time"
)

// ? 超时判断
func test() {
	in := make(chan int)

	select {
	case v := <-in:
		fmt.Println(v)
	case <-time.After(time.Second):
		return // 超时
	}
}

// ? 定时操作
func test2() {
	in := make(chan int)

	ticker := time.NewTicker(time.Second)
	for {
		select {
		case <-in:
			//  退出
			ticker.Stop()
			return
		case <-ticker.C:
			//定时操作
		}
	}
}

// ? 多个任务 - 自动维护
func test3() {
	in := make(chan int)
	in2 := make(chan int)

	ticker := time.NewTicker(time.Second)
	for {
		select {
		case v := <-in:
			in = nil
			fmt.Println(v)
		case v2 := <-in2:
			in2 = nil
			fmt.Println(v2)
		case <-ticker.C:
			if in == nil && in2 == nil {
				// 退出
				ticker.Stop()
				return
			}
		}
	}
}

// ? 阻止函数退出：
func test4() {
	// do some thins
	select {}
}

// ? select和default分支, close(channel)广播退出, sync.WaitGroup 控制清理工作完成
func test5() {

	cancel := make(chan bool)
	var wg sync.WaitGroup

	for i := 0; i < 10; i++ {

		wg.Add(1)
		go func(wg *sync.WaitGroup, cannel chan bool) {
			// 退出 清理工作
			defer func() { time.Sleep(time.Second); wg.Done() }()
			for {
				select {
				default:
					fmt.Println("hello")
					// 正常工作
				case <-cannel:
					// 退出
					return
				}
			}
		}(&wg, cancel)
	}

	time.Sleep(time.Second)

	close(cancel)
	wg.Wait()
}

// ? context包 控制退出
/*
* 不要把Context放在struct中，要以参数的方式传递，parent Context一般为Background
* 应该作为第一个参数, 变量名建议都统一，如ctx。
* 不要传递nil，
* 尽量少用WithValue
* Context是线程安全的，可以放心的在多个goroutine中传递
* 可以把一个 Context 对象传递给任意个数的 gorotuine，对它执行 取消 操作时，所有 goroutine 都会接收到取消信号。
 */
func test6() {

	// ctx, cancelFun := context.WithCancel(context.Background())
	// ctx, cancelFun := context.WithTimeout(context.Background(), time.Minute)
	ctx, cancelFun := context.WithDeadline(context.Background(), time.Now().Add(time.Minute))
	// ctx = context.WithValue(ctx, &ctx, 1234)
	var wg sync.WaitGroup

	for i := 0; i < 10; i++ {

		wg.Add(1)
		go func(ctx context.Context, wg *sync.WaitGroup) {
			// 退出 清理工作
			defer func() { time.Sleep(time.Second); wg.Done() }()
			for {
				select {
				default:
					fmt.Println("hello")
					// 正常工作
				case <-ctx.Done():
					// 退出
					ctx.Err()
					return
				}
			}
		}(ctx, &wg)
	}

	time.Sleep(time.Second)

	cancelFun()
	wg.Wait()
}
