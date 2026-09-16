package com.sjbb.core.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public class HttpRespUtil {

    // 中断请求
    public static void cutRequestHandler(HttpServletResponse response, Object body, HttpStatus status) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(status.value());
        PrintWriter writer = response.getWriter();
        if (Objects.nonNull(body)) {
            writer.print(JSONObject.toJSONString(body));
        }
        writer.close();
        response.flushBuffer();
    }

}
