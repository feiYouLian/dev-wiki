package runner

import (
	"fmt"
	"testing"
)

func TestCancelOrderCheck(t *testing.T) {
	hubconfig, err := InitHubConfig("../conf.ini")
	if err != nil {
		fmt.Println(err)
	}
	type args struct {
		h *HubConfig
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		// TODO: Add test cases.
		{"dev", args{hubconfig}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := CancelOrderCheck(tt.args.h); got != tt.want {
				t.Errorf("CancelOrderCheck() = %v, want %v", got, tt.want)
			}
		})
	}
}
