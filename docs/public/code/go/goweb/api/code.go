package api

import (
	"goweb/db"
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
)

// SaveRule SaveRule
func SaveRule(c *gin.Context) {
	rule := new(db.DemoRule)
	c.ShouldBindJSON(rule)
	_, err := db.Mysql.ID(rule.ID).Update(rule)
	if err != nil {
		log.Println(err)
	}
	c.JSON(http.StatusOK, rule)
}

// GetStatus GetStatus
func GetStatus(c *gin.Context) {
	rule := new(db.DemoRule)
	_, err := db.Mysql.NewSession().ID(1).Get(rule)
	if err != nil {
		log.Println(err)
	}
	c.JSON(http.StatusOK, rule)
}

func encodeBatch(id int64, rule *db.DemoRule) []string {
	indexArr := encodeIndex(id, rule)
	return rule.EncodeBatch(indexArr)
}

func encode(id int64, rule *db.DemoRule) string {
	indexArr := encodeIndex(id, rule)
	return rule.Encode(indexArr)
}

func decode(code string, rule *db.DemoRule) int64 {
	chars := []rune(rule.Chars)
	charsLen := int64(len(chars))
	encodeLen := int64(rule.EncodeLen())
	prime := int64(rule.Prime())
	prime2 := int64(rule.Prime2())
	slat := int64(rule.Slat)
	encode := rule.Decode(code)
	if int64(len(encode)) != encodeLen {
		return -1
	}

	//将字符还原成对应数字
	a := make([]int64, encodeLen)
	for i := int64(0); i < encodeLen; i++ {
		char := encode[i]
		index := findIndex(char, chars)
		if index == -1 {
			//异常字符串
			return -1
		}
		a[i*prime2%encodeLen] = int64(index)
	}

	b := make([]int64, encodeLen)
	for i := encodeLen - 2; i >= 0; i-- {
		b[i] = (a[i] - a[0]*i + charsLen*i) % charsLen
	}

	var res int64
	for i := encodeLen - 2; i >= 0; i-- {
		res += b[i]
		if i > 0 {
			res *= charsLen
			continue
		}
	}
	return (res - slat) / prime
}

func findIndex(char rune, chars []rune) int {
	for i, c := range chars {
		if char == c {
			return i
		}
	}
	return -1
}

func encodeIndex(id int64, rule *db.DemoRule) []int {
	chars := []rune(rule.Chars)
	charsLen := int64(len(chars))
	encodeLen := int64(rule.EncodeLen())
	prime := int64(rule.Prime())
	prime2 := int64(rule.Prime2())
	slat := int64(rule.Slat)

	b := make([]int64, encodeLen)
	var temp, i, j int64

	b[0] = id*prime + slat
	for i = 0; i < encodeLen-1; i++ {
		b[i+1] = b[i] / charsLen
		//按位扩散
		b[i] = (b[i] + i*b[0]) % charsLen
		temp += b[i]
	}
	// 最后一位 校验位
	b[encodeLen-1] = temp * prime % charsLen

	indexArray := make([]int, encodeLen)
	for j = 0; j < encodeLen; j++ {
		indexArray[j] = int(b[j*prime2%encodeLen])
	}
	return indexArray
}
