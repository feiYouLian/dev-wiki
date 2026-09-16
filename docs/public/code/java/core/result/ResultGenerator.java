package com.sjbb.core.result;

import org.apache.commons.lang3.StringUtils;

/**
 * 响应结果生成工具
 */
public class ResultGenerator {
    private static final String DEFAULT_SUCCESS_MESSAGE = "SUCCESS";

    public static Result genSuccessResult() {
        return new Result()
                .setCode(ResultCode.SUCCESS)
                .setMessage(DEFAULT_SUCCESS_MESSAGE);
    }

    public static Result genSuccessResult(Object data) {
        return new Result()
                .setCode(ResultCode.SUCCESS)
                .setMessage(DEFAULT_SUCCESS_MESSAGE)
                .setData(data);
    }

    public static Result genFailResult(String message, int code) {
        return new Result()
                .setCode(code)
                .setMessage(message);
    }

    public static Result genFailResult(String message) {
        return new Result()
                .setCode(ResultCode.FAIL)
                .setMessage(message);
    }

    public static Result genFailResult(ResultCode resultCode) {
        return new Result()
                .setCode(resultCode.getCode())
                .setMessage(resultCode.getMessage());
    }

    public static Result genFailResult(ResultCode resultCode, String msgSuffix) {
        if (StringUtils.isBlank(msgSuffix)) {
            return ResultGenerator.genFailResult(resultCode);
        }
        return new Result()
                .setCode(resultCode.getCode())
                .setMessage(resultCode.getMessage() + msgSuffix);
    }
}
