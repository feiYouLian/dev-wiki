package runner

import (
	"errors"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"market.task/config"

	"github.com/go-redis/redis"
	"github.com/go-xorm/xorm"
	"github.com/jasonlvhit/gocron"

	"market.task/db"
	"market.task/notice"
)

// 0-未启用 1-已启用 2-进行中 3-已结束 4-错误中止
const (
	// 任务未加载
	StateUnload = iota
	// 已加载到内存，准备运行
	Stateload
	// 运行中
	StateRuning
	// 运行停止
	StateStop
	// 运行出错停止
	StateErr
)

const (
	// LogInfo set log level info
	LogInfo = "info"
	// LogWarn set log level warn
	LogWarn = "warn"
	// LogErr set log level error
	LogErr = "error"
)

// Hub is a task management center, it is for persisting task info into the database.
type Hub struct {
	Runners map[string]*Runner
	//Logs is a writing chan which is used for reading log from task
	LogChan   chan *runnerLog
	StateChan chan *runnerState
	Config    *HubConfig
	exit      bool
}

// HubConfig is hub config for newing a hub
type HubConfig struct {
	//base database
	MysqlDb *xorm.Engine
	Redis   *redis.Client
	Notice  *notice.DingNotice
}

type runnerLog struct {
	runner string
	level  string
	log    string
}

// RLog runner log
type RLog struct {
	level string
	log   string
}

type runnerState struct {
	runner string
	state  uint8
}

// NewHub new a Hub instance
func NewHub(config *HubConfig) *Hub {
	return &Hub{
		Runners:   make(map[string]*Runner, 10),
		LogChan:   make(chan *runnerLog, 50),
		StateChan: make(chan *runnerState, 50),
		Config:    config,
	}
}

// Start all task
func (h *Hub) Start() {
	sc := make(chan os.Signal, 1)
	signal.Notify(sc, os.Interrupt)

	go h.handleInterrupt(sc)

	go h.handleRunnerState()
	go h.handleRunnerLog()

	<-gocron.Start()
	log.Println("task is running")
}

// handleInterrupt 处理程序退出中断
func (h *Hub) handleInterrupt(sc chan os.Signal) {
	select {
	case <-sc:
		log.Println("system is stoping")
		h.exit = true
		for {
			isrun := false
			for _, r := range h.Runners {
				if r.State == StateRuning {
					isrun = true
				}
			}
			if !isrun {
				h.Config.MysqlDb.Cols("state").Update(&db.Runner{State: StateUnload})
				log.Println("system exit")
				syscall.Exit(1)
			}
			time.Sleep(time.Millisecond * 100)
		}
	}
}

func (h *Hub) createLogChan(runner string) chan *RLog {
	ch := make(chan *RLog)

	go func() {
		for {
			select {
			case log := <-ch:
				h.LogChan <- &runnerLog{runner: runner, level: log.level, log: log.log}
			}
		}
	}()

	return ch
}

// AddRunner add a runner, runner 默认开始时间为空，间隔为10s.
func (h *Hub) AddRunner(runner *Runner) {
	baseDb := h.Config.MysqlDb
	var r = db.Runner{Name: runner.Name}
	has, err := baseDb.Get(&r)
	if err != nil {
		log.Println(err)
	}
	if !has {
		// 写记录到数据库中
		r.State = StateUnload
		r.StartAt = "-"
		r.Interval = 10
		r.CreatedAt = time.Now()
		aff, _ := baseDb.Insert(&r)
		if aff == 0 {
			log.Println("inserting a runner to db is failed")
			return
		}
	} else {
		r.State = Stateload
		baseDb.ID(r.ID).Update(&r)
	}
	log.Println("检测到任务, Name:", r.Name, "StartAt:", r.StartAt, "Interval:", r.Interval)

	runner.R = &r

	runfunc := func(logs chan *RLog) {
		if h.exit {
			return
		}
		defer func() {
			if err := recover(); err != nil {
				log.Println(err)
				logs <- &RLog{level: LogErr, log: fmt.Sprint(err)}
				h.StateChan <- &runnerState{runner: runner.Name, state: StateErr}
			}
		}()

		if runner.State == StateRuning || !runner.Ready(h.Config) {
			return
		}

		runner.State = StateRuning
		h.StateChan <- &runnerState{runner: runner.Name, state: StateRuning}

		err := runner.Run(h.Config, logs)
		if err != nil {
			logs <- &RLog{LogErr, err.Error()}
			runner.State = StateErr
		} else {
			runner.State = StateStop
		}

		h.StateChan <- &runnerState{runner: runner.Name, state: runner.State}
	}
	//创建 job 对象
	job := gocron.Every(uint64(r.Interval)).Seconds()
	if r.StartAt != "-" && r.StartAt != "" {
		job.At(r.StartAt)
	}

	job.Do(runfunc, h.createLogChan(runner.Name))
	h.Runners[runner.Name] = runner

}

func (h *Hub) handleRunnerLog() {
	baseDb := h.Config.MysqlDb
	for {
		select {
		case logobj := <-h.LogChan:
			runner := h.Runners[logobj.runner]
			log.Println(logobj.runner, logobj.log)
			// save to db
			runnerLog := db.RunnerLog{
				RunnerID:  runner.R.ID,
				Level:     logobj.level,
				Log:       logobj.log,
				CreatedAt: time.Now(),
			}
			_, err := baseDb.InsertOne(runnerLog)
			if err != nil {
				log.Println(err)
			}
		}
	}
}

func (h *Hub) handleRunnerState() {
	baseDb := h.Config.MysqlDb
	var latestNotifyTime time.Time
	for {
		select {
		case st := <-h.StateChan:
			runner := h.Runners[st.runner]
			if st.state == StateErr {
				if time.Now().Sub(latestNotifyTime) > time.Minute*30 {
					h.Config.Notice.SendText("dev", fmt.Sprintf("任务错误中止: %v", runner.Name))
					latestNotifyTime = time.Now()
				} else {
					log.Printf("任务错误中止: %v", runner.Name)
				}
			}
			// log.Println(st.runner, ";state:", st.state)
			// save to db
			runner.R.State = st.state
			runner.R.UpdatedAt = time.Now()
			baseDb.ID(runner.R.ID).Update(runner.R)
		}
	}
}

// Runner 定义了任务接口实现
type Runner struct {
	R    *db.Runner
	Name string
	//状态 0-未启用 1-已启用 2-进行中 3-已结束 4-错误中止
	State uint8
	// 判断这个任务数据是否达到执行的要求，可以开始调用 Run 去执行
	Ready func(h *HubConfig) bool
	// Run 为任务核心方法，返回一个chan。
	// 可以读取到任务的日志信息
	Run func(h *HubConfig, logs chan *RLog) error
}

// NewDefaultRunner return a Runner instance which check prop is a default func
func NewDefaultRunner(name string, runfunc func(h *HubConfig, logs chan *RLog) error) *Runner {
	return &Runner{
		Name:  name,
		State: StateUnload,
		Run:   runfunc,
		Ready: func(h *HubConfig) bool {
			return true
		},
	}
}

// NewRunner return a Runner instance
func NewRunner(name string, checkfunc func(h *HubConfig) bool, runfunc func(h *HubConfig, logs chan *RLog) error) *Runner {
	return &Runner{
		Name:  name,
		State: StateUnload,
		Ready: checkfunc,
		Run:   runfunc,
	}
}

// InitHubConfig InitHubConfig
func InitHubConfig(configFile string) (*HubConfig, error) {
	if configFile == "" {
		configFile = config.DefaultConfigFile
	}
	cfg, err := config.LoadConfig(configFile)
	if err != nil {
		return nil, errors.New("config read failed")
	}

	redis := db.InitRedis(cfg.Redis.IP, cfg.Redis.Port, cfg.Redis.Password)

	mysql := db.InitDb(cfg.Mysql.IP, cfg.Mysql.Port, cfg.Mysql.User, cfg.Mysql.Password, cfg.Mysql.DbName)

	devGroup := notice.DingNoticeGroup{
		Name:    "dev",
		Webhook: cfg.Notify.Webhook,
		At:      []string{cfg.Notify.DevAt},
	}
	dingNotice, err := notice.NewNotice(devGroup)
	if err != nil {
		return nil, err
	}
	return &HubConfig{MysqlDb: mysql, Redis: redis, Notice: dingNotice}, nil
}
