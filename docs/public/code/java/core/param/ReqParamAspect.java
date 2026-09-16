package com.sjbb.core.param;


import com.sjbb.core.result.ResultCode;
import com.sjbb.core.result.ResultGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Order(0)
public class ReqParamAspect {

    @Pointcut("execution(* com.sjbb..web..*.*(..))")
    public void paramCut() {
    }

    @Autowired
    HttpServletRequest request;


    @Around(value = "paramCut()")
    public Object verification(ProceedingJoinPoint point) throws Throwable {
        if (point.getArgs().length > 0) {
            for (int i = 0; i < point.getArgs().length; i++) {
                Object param = point.getArgs()[i];
                ResultCode resultCode = ReqParamCheckUtils.check(param);
                if (!ResultCode.SUCCESS.equals(resultCode)) {
                    return ResultGenerator.genFailResult(resultCode);
                }
            }
        }
        return point.proceed();
    }

}
