package notice

import (
	"testing"
)

const webhook = "https://oapi.dingtalk.com/robot/send?access_token=b6645bb3a11852b12d1da4f84620cfeb5737ec1ce286d88663d57c117e8da0d1"

func TestNotice_SendDingTextNotice(t *testing.T) {
	groups := []DingNoticeGroup{{
		Name:    "sport_mapping",
		Webhook: webhook,
		At:      []string{"17612103683"},
	}}

	notice, err := NewNotice(groups...)
	if err != nil {
		t.Error(err)
		return
	}
	notice.SendText("sport_mapping", "test 测试")
}
