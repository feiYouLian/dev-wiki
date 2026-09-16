package config

import (
	"fmt"
	"path/filepath"

	"github.com/go-ini/ini"
)

const (
	defaultConfigName = "conf.ini"
)

var (
	defaultDir = "./"
	// DefaultConfigFile DefaultConfigFile
	DefaultConfigFile = filepath.Join(defaultDir, defaultConfigName)
)

// RedisConfig redis config
type RedisConfig struct {
	IP       string
	Port     int
	Password string
}

// MysqlConfig mysql config
type MysqlConfig struct {
	IP       string
	Port     int
	User     string
	Password string
	DbName   string
}

// NotifyConfig Notify config
type NotifyConfig struct {
	Webhook     string
	OperationAt string
	DevAt       string
}

// Config 系统配置信息，包括 redis 配置，
type Config struct {
	Redis  RedisConfig
	Mysql  MysqlConfig
	Notify NotifyConfig
}

// LoadConfig LoadConfig
func LoadConfig(configFile string) (*Config, error) {
	cfg, err := ini.Load(configFile)
	if err != nil {
		fmt.Printf("Fail to read file: %v", err)
		return nil, err
	}
	var config Config

	section := cfg.Section("redis")
	config.Redis = RedisConfig{}
	err = section.MapTo(&config.Redis)
	if err != nil {
		return nil, err
	}

	section = cfg.Section("mysql")
	config.Mysql = MysqlConfig{}
	err = section.MapTo(&config.Mysql)
	if err != nil {
		return nil, err
	}

	section = cfg.Section("notify")
	config.Notify = NotifyConfig{}
	err = section.MapTo(&config.Notify)
	if err != nil {
		return nil, err
	}

	return &config, nil
}
