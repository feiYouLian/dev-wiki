package api

import (
	"fmt"
	"goweb/db"
	"reflect"
	"testing"
)

func Test_encode(t *testing.T) {
	rule := new(db.DemoRule)
	db.Mysql.NewSession().ID(1).Get(rule)

	type args struct {
		id   int64
		rule *db.DemoRule
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
		{"1", args{1, rule}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var got string
			if got = encode(tt.args.id, tt.args.rule); !reflect.DeepEqual(decode(got, tt.args.rule), tt.args.id) {
				t.Errorf("encode() = %v, want %v", got, tt.args.id)
			}
			fmt.Println(tt.args.rule.UpperLimitID())
			fmt.Printf("encode() = %v, want %v\n", got, tt.args.id)
		})
	}
}
