package com.sjbb.core.param;

import com.sjbb.core.result.ResultCode;
import com.sjbb.core.utils.SpringContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.regex.Pattern;

public class ReqParamCheckUtils {

    public static ResultCode check(Object param) throws Exception {
        ResultCode resultCode = ResultCode.SUCCESS;
        if (!needToCheck(param)) {
            return resultCode;
        }
        if (param instanceof Collection<?>) {
            Collection<?> params = (Collection<?>) param;
            if (!params.isEmpty()) {
                for (Object element : params) {
                    resultCode = check(element);
                    if (!ResultCode.SUCCESS.equals(resultCode)) {
                        return resultCode;
                    }
                }
            }
            return resultCode;
        }

        Class<?> clazz = param.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(param);
            ReqParamRule[] rules = field.getAnnotationsByType(ReqParamRule.class);
            resultCode = doCheck(param, value, rules);
            if (!ResultCode.SUCCESS.equals(resultCode)) {
                return resultCode;
            }
            // value 递归检查
            if (needToCheck(value)) {
                resultCode = check(value);
                if (!ResultCode.SUCCESS.equals(resultCode)) {
                    return resultCode;
                }
            }
        }
        return ResultCode.SUCCESS;
    }


    private static ResultCode doCheck(Object param, Object value, ReqParamRule... rules) throws Exception {
        for (ReqParamRule anno : rules) {
            if (anno.required() && isEmpty(value)) {
                return anno.result();
            }
            if (StringUtils.isNotBlank(anno.patten()) && !isEmpty(value) &&
                    !Pattern.matches(anno.patten(), String.valueOf(value))) {
                return anno.result();
            }
            String[] methods = anno.dycMethods();
            for (String method : methods) {
                if (!executeMethod(method, value, param)) {
                    return anno.result();
                }
            }
        }
        return ResultCode.SUCCESS;
    }

    /**
     * 判断类型是否是需要检查
     * param 三种情况不需要检查
     * 1. 为空
     * 2. 是 MultipartFile 类型
     * 3. 是 java 原生类型(不是Collection的)
     *
     * @param param 参数
     * @return 是否需要检查
     */
    private static boolean needToCheck(Object param) {
        return null != param &&
                !(param instanceof MultipartFile || param instanceof MultipartFile[]) &&
                (!isJavaClass(param.getClass()) || param instanceof Collection<?>);
    }

    private static boolean isJavaClass(Class<?> clz) {
        return clz != null && clz.getClassLoader() == null;
    }

    private static boolean executeMethod(String method, Object field, Object arg) throws Exception {
        if (StringUtils.isBlank(method) || field == null) {
            return true;
        }
        String[] strings = method.split(".");
        String className = strings[0];
        String methodName = strings[1];

        Class<?> cls = Class.forName(className);
        Object bean;
        try {
            bean = SpringContextUtils.getBean(cls);
        } catch (Exception e) {
            bean = cls.newInstance();
        }

        try {
            Method m2 = cls.getDeclaredMethod(methodName, field.getClass());
            return (boolean) m2.invoke(bean, field);
        } catch (NoSuchMethodException e) {
            Method m3 = cls.getDeclaredMethod(methodName, field.getClass(), arg.getClass());
            return (boolean) m3.invoke(bean, field, arg);
        }
    }

    private static boolean isEmpty(Object o) {
        if (o instanceof String) {
            return StringUtils.isBlank((String) o);
        } else if (o instanceof Collection) {
            Collection<?> collection = (Collection<?>) o;
            return collection.isEmpty();
        }
        return null == o;
    }

}
