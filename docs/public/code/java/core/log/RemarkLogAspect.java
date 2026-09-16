package com.sjbb.core.log;

import com.alibaba.fastjson.JSON;
import com.sjbb.core.model.dto.LogDTO;
import com.sjbb.core.model.dto.TokenUserDTO;
import com.sjbb.core.result.Result;
import com.sjbb.core.token.TokenHolder;
import com.sjbb.core.utils.IPUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

@Aspect
@Component
@Order(9)
public class RemarkLogAspect {

    @Autowired
    HttpServletRequest request;

    private final Logger logger = LoggerFactory.getLogger(RemarkLogAspect.class);

    @Pointcut("@annotation(com.sjbb.core.log.RemarkLog)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Result result = null;
        try {
            // 执行方法
            result = (Result) point.proceed();
        } finally {
            // 执行时长(毫秒)
            long time = System.currentTimeMillis() - beginTime;
            // 异步保存日志
            saveLog(point, time, null != result ? result.getMessage() : "");
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, long time, String result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogDTO logDTO = new LogDTO();
        RemarkLog syslog = AnnotationUtils.findAnnotation(method, RemarkLog.class);
        if (syslog != null) {
            // 注解上的描述
            logDTO.setDescription(syslog.description());
        }
        // 请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        logDTO.setMethod(className + "." + methodName + "()");
        // 请求的参数
        Object[] args = joinPoint.getArgs();
        StringBuilder params = new StringBuilder();
        if (args.length > 0) {
            for (Object arg : args) {
                params.append(",");
                if (arg instanceof MultipartFile) {
                    MultipartFile file = (MultipartFile) arg;
                    params.append(file.getOriginalFilename());
                } else {
                    params.append(JSON.toJSONString(arg));
                }
            }
            logDTO.setParams(params.delete(0, 1).toString());
        }
        // 设置IP地址
        logDTO.setIp(IPUtils.getIpAddr(request));
        // 用户名
        TokenUserDTO user = TokenHolder.getUser();
        logDTO.setUserId(user == null ? null : user.getUserId());
        logDTO.setUsername(user == null ? null : user.getNickname());
        logDTO.setTime((int) time);
        // 系统当前时间
        Date date = new Date();
        logDTO.setOperationTime(date);
        // 结果
        logDTO.setResult(result);
        // 保存系统日志
        logger.info("request log: {}", JSON.toJSONString(logDTO));
    }
}
