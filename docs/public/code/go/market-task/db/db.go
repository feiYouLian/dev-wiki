package db

import (
	"fmt"
	"log"
	"time"

	"github.com/go-redis/redis"
	// mysql driver
	_ "github.com/go-sql-driver/mysql"
	"github.com/go-xorm/xorm"
	"xorm.io/core"
)

// Runner define a table named runner,which is used for a task recording
type Runner struct {
	ID   int64 `xorm:"pk autoincr 'id'"`
	Name string
	Desc string `xorm:"'desc'"`
	// 开始时间 格式： 11:00  时:分
	StartAt string `xorm:"time"`
	// 间隔时间。单位 秒
	Interval  int
	State     uint8
	CreatedAt time.Time
	UpdatedAt time.Time
}

// RunnerLog define a runner log table
type RunnerLog struct {
	ID        int64  `xorm:"pk autoincr 'id'"`
	RunnerID  int64  `xorm:"'runner_id'"`
	Log       string `xorm:"varchar(80) "`
	Level     string `xorm:"varchar(10) default 'info'"`
	CreatedAt time.Time
}

// TableName return TableName
func (Runner) TableName() string {
	return "t_system_runner"
}

// TableName return TableName
func (RunnerLog) TableName() string {
	return "t_system_runner_log"
}

// InitDb init database
func InitDb(ip string, port int, user, pwd, dbname string) *xorm.Engine {
	url := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?charset=utf8&parseTime=True&loc=Local", user, pwd, ip, port, dbname)
	engine, err := xorm.NewEngine("mysql", url)
	if err != nil {
		log.Println(err)
		return nil
	}

	// engine.ShowSQL(true)

	tbMapper := core.NewPrefixMapper(core.SnakeMapper{}, "t_")
	engine.SetTableMapper(tbMapper)

	engine.SetColumnMapper(core.SnakeMapper{})

	return engine
}

// InitRedis return a redis instance
func InitRedis(ip string, port int, pass string) *redis.Client {
	redisdb := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%d", ip, port),
		Password: pass,
	})
	_, err := redisdb.Ping().Result()
	if err != nil {
		log.Println(err)
	}
	return redisdb
}
