package db

import (
	"fmt"
	"goweb/config"
	"log"

	"github.com/go-xorm/xorm"
	"xorm.io/core"

	// mysql driver
	_ "github.com/go-sql-driver/mysql"
)

var (
	// Mysql Mysql
	Mysql *xorm.Engine
)

func init() {
	cfg := config.MysqlConfig
	url := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?charset=utf8&parseTime=True&loc=Local", cfg.Username, cfg.Passward, cfg.IP, cfg.Port, cfg.Dbname)
	mysql, err := xorm.NewEngine("mysql", url)
	if err != nil {
		log.Panic(err)
		return
	}
	// mysql.ShowSQL(true)

	tbMapper := core.NewPrefixMapper(core.SnakeMapper{}, "t_")
	mysql.SetTableMapper(tbMapper)

	mysql.SetColumnMapper(core.SnakeMapper{})
	Mysql = mysql
}
