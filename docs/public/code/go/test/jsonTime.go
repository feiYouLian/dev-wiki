package test

import (
	"fmt"
	"time"
)

type jsonTime time.Time

const (
	timeFormart = "2006-01-02 15:04:05"
)

func (jt *jsonTime) UnmarshalJSON(data []byte) (err error) {
	now, err := time.ParseInLocation(`"`+timeFormart+`"`, string(data), time.Local)
	*jt = jsonTime(now)
	return
}

func (jt jsonTime) MarshalJSON() ([]byte, error) {
	// b := make([]byte, 0, len(timeFormart)+2)
	// b = append(b, '"')
	// b = time.Time(t).AppendFormat(b, timeFormart)
	// b = append(b, '"')
	// return b, nil
	var stamp = fmt.Sprintf("\"%s\"", time.Time(jt).Format("2006-01-02 15:04:05"))
	return []byte(stamp), nil
}

func (jt jsonTime) String() string {
	return time.Time(jt).Format(timeFormart)
}
