package config

import (
	"log"
	"os"
	"path/filepath"

	"github.com/spf13/viper"
)

var (
	// MysqlConfig MysqlConfig
	MysqlConfig = &struct {
		IP       string `json:"ip,omitempty"`
		Port     int    `json:"port,omitempty"`
		Dbname   string `json:"dbname,omitempty"`
		Username string `json:"username,omitempty"`
		Passward string `json:"passward,omitempty"`
	}{}
)

const (
	defaultConfigName = "config.yaml"
	defaultDir        = "../"
)

func init() {
	var defaultConfigPath string

	_, err := os.Stat(defaultConfigName)
	if err != nil && !os.IsExist(err) {
		defaultConfigPath = filepath.Join(defaultDir, defaultConfigName)
	} else {
		defaultConfigPath = defaultConfigName
	}
	f, err := os.Open(defaultConfigPath)
	if err != nil {
		log.Panic(err)
	}
	viper.SetConfigType("yaml")
	err = viper.ReadConfig(f)
	if err != nil {
		log.Panic(err)
	}
	viper.UnmarshalKey("mysql", MysqlConfig)
	log.Println(MysqlConfig.IP)
}
