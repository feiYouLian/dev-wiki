package com.sjbb.core.utils;

import java.util.List;

public class PageResp<T> {

    private Long total;

    private List<T> list;

    public PageResp() {
    }

    public PageResp(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
