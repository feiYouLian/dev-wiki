package runner

import (
	"log"
	"time"

	"market.task/db"
)

const updateInventorySQL = "UPDATE t_goods_spec_rel r SET r.inventory = r.inventory + ?, update_time = ? WHERE id = ?"

// CancelOrderCheck CancelOrderCheck
func CancelOrderCheck(h *HubConfig) bool {
	mysqlDB := h.MysqlDb
	count, err := mysqlDB.Where("status =1 AND create_time < ?", time.Now().Add(-3*time.Minute)).Count(new(db.OrderInfo))
	if err != nil {
		log.Println(err)
	}
	return count > 0
}

// CancelOrderRun CancelOrderRun
func CancelOrderRun(h *HubConfig, logs chan *RLog) error {
	mysqlDB := h.MysqlDb
	var orders []db.OrderInfo
	session := mysqlDB.NewSession()
	err := session.Where("status =1 AND create_time < ?", time.Now().Add(-30*time.Minute)).Find(&orders)
	if err != nil {
		return err
	}
	var cancelOrderIds []int64
	for _, order := range orders {
		cancelOrderIds = append(cancelOrderIds, order.ID)
	}
	var oGoods []db.OrderGoods
	err = session.In("order_id", cancelOrderIds).Cols("goods_spec_id", "amount").Find(&oGoods)
	if err != nil {
		session.Rollback()
		return err
	}
	var updateGoodsMap = make(map[int64]int, 0)
	for _, oGood := range oGoods {
		if val, has := updateGoodsMap[oGood.GoodsSpecID]; has {
			updateGoodsMap[oGood.GoodsSpecID] = val + oGood.Amount
		} else {
			updateGoodsMap[oGood.GoodsSpecID] = oGood.Amount
		}
	}
	for goodsSpecID, inventory := range updateGoodsMap {
		timeStr := time.Now().Format("2006-01-02 15:04:05")
		_, err = session.Exec(updateInventorySQL, inventory, timeStr, goodsSpecID)
		if err != nil {
			session.Rollback()
			return err
		}
	}
	_, err = session.In("id", cancelOrderIds).Cols("status", "update_time").Update(&db.OrderInfo{Status: 0, UpdateTime: time.Now()})
	if err != nil {
		session.Rollback()
		return err
	}

	if err := session.Commit(); err != nil {
		session.Rollback()
		return err
	}
	return nil
}
