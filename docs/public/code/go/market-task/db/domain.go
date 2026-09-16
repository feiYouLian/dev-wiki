package db

import "time"

// OrderInfo OrderInfo
type OrderInfo struct {
	ID            int64     `xorm:"pk autoincr BIGINT(20) 'id'"`
	OrderNo       string    `xorm:"not null VARCHAR(11) 'order_no'"`
	UserID        int64     `xorm:"not null BIGINT(20) 'user_id'"`
	UserCouponID  int64     `xorm:"BIGINT(20) 'user_coupon_id'"`
	CouponOffer   int       `xorm:"INT(11)"`
	CostTotal     int       `xorm:"not null INT(11)"`
	DeliverFee    int       `xorm:"not null INT(11)"`
	CostActual    int       `xorm:"INT(11)"`
	PayWay        int       `xorm:"TINYINT(4)"`
	PayTime       time.Time `xorm:"DATETIME"`
	PayTradeNo    string    `xorm:"VARCHAR(255)"`
	Status        int       `xorm:"not null comment('订单状态 0-取消 1-待支付 2-待配送 3-配送中 4-完成') TINYINT(4)"`
	AfterService  int       `xorm:"comment('售后服务状态 1-申请退款 2-退款完成 3-拒绝') TINYINT(4)"`
	CommentStatus int       `xorm:"comment('评论状态 1-已评论 其他-未评论') TINYINT(4)"`
	Remark        string    `xorm:"VARCHAR(500)"`
	RefundRsn     string    `xorm:"VARCHAR(500)"`
	RefuseRsn     string    `xorm:"VARCHAR(500)"`
	CreateTime    time.Time `xorm:"not null DATETIME"`
	UpdateTime    time.Time `xorm:"not null DATETIME"`
}

// OrderGoods OrderGoods
type OrderGoods struct {
	ID          int64  `xorm:"pk autoincr BIGINT(20) 'id'"`
	OrderID     int64  `xorm:"not null BIGINT(20) 'order_id'"`
	GoodsID     int64  `xorm:"not null BIGINT(20) 'goods_id'"`
	GoodsName   string `xorm:"not null VARCHAR(50)"`
	GoodsPicURL string `xorm:"not null VARCHAR(200) 'goods_pic_url'"`
	GoodsSpecID int64  `xorm:"not null BIGINT(20) 'goods_spec_id'"`
	Amount      int    `xorm:"not null INT(11)"`
	Price       int    `xorm:"not null INT(10)"`
	GoodsJSON   string `xorm:"VARCHAR(500) 'goods_json'"`
}

// GoodsSpecRel GoodsSpecRel
type GoodsSpecRel struct {
	ID         int64     `xorm:"pk autoincr BIGINT(20) 'id'"`
	GoodsID    int64     `xorm:"not null BIGINT(20) 'goods_id'"`
	SpecID     int64     `xorm:"not null BIGINT(20) 'spec_id'"`
	PriceOrig  int       `xorm:"not null INT(11)"`
	PriceDisc  int       `xorm:"not null INT(11)"`
	Inventory  int       `xorm:"not null INT(11)"`
	CreateTime time.Time `xorm:"not null DATETIME"`
	UpdateTime time.Time `xorm:"not null DATETIME"`
}
