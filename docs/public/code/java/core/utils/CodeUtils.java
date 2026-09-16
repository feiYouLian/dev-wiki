package com.sjbb.core.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;

public class CodeUtils {

    public static int buildNum(String version) {
        if (StringUtils.isBlank(version)) {
            return 0;
        }
        DecimalFormat df = new DecimalFormat("000");
        StringBuilder sb = new StringBuilder();
        String[] vers = version.split("\\.");
        for (int i = 0; i < vers.length; i++) {
            if (i == 0) {
                sb.append(vers[i]);
                continue;
            }
            sb.append(df.format(Integer.valueOf(vers[i])));
        }
        return Integer.valueOf(sb.toString());
    }
}
