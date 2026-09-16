package com.sjbb.core.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sjbb.core.param.ReqParamRule;
import com.sjbb.core.result.ResultCode;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class PageQuery implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6113277744104239499L;

	@ReqParamRule(result = ResultCode.FAIL,patten = "[1-9]\\d*|0")
	private Integer index=0;// 页数
	@ReqParamRule(result = ResultCode.FAIL,patten = "^100$|^(\\d|[1-9]\\d)$")
	private Integer count=10;// 每页条数
	private String sort; // like: id_asc|name_desc
	public Integer getIndex() {
		return index;
	}
	public void setIndex(Integer index) {
		this.index = index;
	}
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}
	public String getSort() {
		return sort;
	}
	public void setSort(String sort) {
		this.sort = sort;
	}
	
	@JsonIgnore
	public String getSqlSort() {
		if(StringUtils.isBlank(sort)){
			return sort;
		}
		String[] sorts = sort.split("_");
		return sorts[0].replaceAll("[A-Z]", "_$0").toLowerCase()+" "+sorts[1];
	}
	
	
}	
