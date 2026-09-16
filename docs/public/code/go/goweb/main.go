package main

import (
	"goweb/api"
	"net/http"

	"github.com/gin-gonic/gin"
)

func main() {
	router := gin.Default()
	router.Use(corsMiddleware())
	router.GET("/ping", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "pong",
		})
	})
	router.GET("/code/getStatus", api.GetStatus)
	router.Run(":5000")
}

func corsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// 这是允许访问所有域
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		// 允许访问的 header信息,*表示全部
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, accesstoken")
		//允许提交请求的方法，*表示全部允许
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET")
		// 预检请求的缓存时间（秒），即在这个时间段里，对于相同的跨域请求不会再预检了
		c.Header("Access-Control-Max-Age", "18000")
		// 是否允许cookies跨域, 默认设置为true
		// c.Header("Access-Control-Allow-Credentials", "false")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	}
}
