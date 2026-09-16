package db

import (
	"math"
	"math/rand"
	"strings"
)

// DemoRule DemoRule
type DemoRule struct {
	ID          int64 `xorm:"pk autoincr BIGINT(20) 'id'"`
	Chars       string
	CodeLen     int
	Slat        int64
	Prefix      string
	Suffix      string
	RandomRate  int
	BatchAmount int
	Status      bool
	Digest      string
}

func (r *DemoRule) randomIndex() int {
	return rand.Intn(len(r.Chars))
}

// EncodeLen EncodeLen
func (r *DemoRule) EncodeLen() int {
	len := r.CodeLen - len(r.Prefix) - len(r.Suffix)
	return len - len/r.RandomRate
}

// Prime GetPrime
func (r *DemoRule) Prime() int {
	i := 2
	charsLen := len(r.Chars)
	for ; charsLen%i == 0; i++ {
	}
	return i
}

// Prime2 GetPrime2
func (r *DemoRule) Prime2() int {
	return 2*r.EncodeLen() - 1
}

// Decode Decode
func (r *DemoRule) Decode(code string) []rune {
	code = strings.TrimPrefix(code, r.Prefix)
	code = strings.TrimSuffix(code, r.Suffix)
	temps := []rune(code)
	var codes []rune
	for i := 0; i < len(temps); i++ {
		if (i+1)%r.RandomRate != 0 {
			codes = append(codes, temps[i])
		}
	}
	return codes
}

// Encode Encode
func (r *DemoRule) Encode(encodeIndex []int) string {
	var codes []rune
	chars := []rune(r.Chars)
	for i := 0; i < len(encodeIndex); i++ {
		codes = append(codes, chars[encodeIndex[i]])
		if (len(codes)+1)%r.RandomRate == 0 {
			codes = append(codes, chars[r.randomIndex()])
		}
	}
	return r.Prefix + string(codes) + r.Suffix
}

// EncodeBatch EncodeBatch
func (r *DemoRule) EncodeBatch(encodeIndex []int) []string {
	set := make(map[string]bool)
	for len(set) < r.BatchAmount {
		set[r.Encode(encodeIndex)] = false
	}
	var keys []string
	for key := range set {
		keys = append(keys, key)
	}
	return keys
}

// UpperLimitID UpperLimitID
func (r *DemoRule) UpperLimitID() int64 {
	charsLen := float64(len(r.Chars))
	encodeLen := float64(r.EncodeLen())
	slat := float64(r.Slat)
	prime := int64(r.Prime())
	return int64(math.Floor(math.Pow(charsLen, encodeLen-1)-slat))/prime - 1
}

// DemoIndex DemoIndex
type DemoIndex struct {
	ID     int64 `xorm:"pk autoincr BIGINT(20) 'id'"`
	Status bool
}

// DemoCode DemoCode
type DemoCode struct {
	ID         int64 `xorm:"pk autoincr BIGINT(20) 'id'"`
	Num        int64
	RandomCode string
}
