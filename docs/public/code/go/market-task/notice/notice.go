package notice

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
)

// DingNotice 通知服务
type DingNotice struct {
	groups map[string]DingNoticeGroup
}

// DingNoticeGroup 钉钉群配置
type DingNoticeGroup struct {
	Name    string
	Webhook string
	At      []string
}

type dingText struct {
	Content string `json:"content"`
}
type dingAt struct {
	AtMobiles []string `json:"atMobiles"`
	IsAtAll   bool     `json:"isAtAll"`
}

// dingTextMsg 文件
type dingTextMsg struct {
	Msgtype string   `json:"msgtype"`
	Text    dingText `json:"text"`
	At      dingAt   `json:"at"`
}

// NewNotice new notice
func NewNotice(groups ...DingNoticeGroup) (*DingNotice, error) {
	if len(groups) == 0 {
		return nil, fmt.Errorf("Group is required")
	}

	groupcache := make(map[string]DingNoticeGroup, len(groups))

	for _, group := range groups {
		if _, ok := groupcache[group.Name]; ok {
			return nil, fmt.Errorf("Group Name repeat")
		}
		groupcache[group.Name] = group
	}
	return &DingNotice{groups: groupcache}, nil
}

// SendText 发送钉钉文本通知
func (t *DingNotice) SendText(groupName, content string) error {
	group, ok := t.groups[groupName]
	if !ok {
		return fmt.Errorf("Group [%s] no found", groupName)
	}
	msg := dingTextMsg{
		Msgtype: "text",
	}
	if len(group.At) > 0 {
		msg.At.AtMobiles = group.At
		for _, mobile := range group.At {
			content = "@" + mobile + " " + content
		}
	}
	msg.Text = dingText{content}
	msgbytes, err := json.Marshal(msg)
	log.Println(string(msgbytes))
	if err != nil {
		return err
	}
	if err := SendNotice(group, bytes.NewReader(msgbytes)); err != nil {
		return err
	}
	return nil
}

// SendNotice 发送钉钉群通知
func SendNotice(group DingNoticeGroup, msg io.Reader) error {

	resp, err := http.Post(group.Webhook, "application/json", msg)
	if err != nil {
		return err
	}
	if resp.StatusCode != 200 {
		return fmt.Errorf("reps status :%d", resp.StatusCode)
	}
	return nil
}
