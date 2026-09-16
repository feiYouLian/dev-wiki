package com.wins.utils;

import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomCodeUtils {

    /**
     * 随机字符串,去掉容易混淆的，O、0、I、1等字符 剩下32位
     */
    private static final char[] DEFAULT_CHARS = new char[] { 'F', 'L', 'G', 'W', '5', 'X', 'C', '3', '9', 'Z', 'M', '6',
            '7', 'Y', 'R', 'T', '2', 'H', 'S', '8', 'D', 'V', 'E', 'J', '4', 'K', 'Q', 'P', 'U', 'A', 'N', 'B' };

    /**
     * 码长度,
     */
    private final static int DEFAULT_CODE_LEN = 9;

    /**
     * 随机数据
     */
    private final static long DEFAULT_SLAT = 1234567L;

    /**
     * 默认规则
     */
    public static final Rule DEFAULT_RULE = new Rule(DEFAULT_CHARS, DEFAULT_CODE_LEN, DEFAULT_SLAT);

    public static String encode(long id, Rule rule) {

        assert !RuleChanged(rule) : "规则已被修改";

        char[] chars = rule.getChars();
        int charsLen = chars.length;
        int encodeLen = rule.getEncodeLen();
        int prime1 = rule.getPrime();
        int prime2 = rule.getPrime2();
        long slat = rule.getSlat();
        // 补位
        id = id * prime1 + slat;
        // 将 id 转换成32进制的值
        Long[] b = new Long[encodeLen];
        // 32进制数
        b[0] = id;
        long temp = 0L;
        for (int i = 0; i < encodeLen - 1; i++) {
            b[i + 1] = b[i] / charsLen;
            // 按位扩散
            b[i] = (b[i] + i * b[0]) % charsLen;
            temp += b[i];
        }
        // 最后一位 校验位
        b[encodeLen - 1] = temp * prime1 % charsLen;

        // 进行混淆
        int[] codeIndexArray = new int[encodeLen];
        for (int i = 0; i < encodeLen; i++) {
            codeIndexArray[i] = b[i * prime2 % encodeLen].intValue();
        }
        return rule.insertEncode(codeIndexArray);
    }

    public static Long decode(String code, Rule rule) {
        assert !RuleChanged(rule) : "规则已被修改";
        int encodeLen = rule.getEncodeLen();
        String encode = rule.extractEncode(code);
        if (encode.length() != encodeLen) {
            return null;
        }
        char[] chars = rule.getChars();
        int charsLen = chars.length;
        int prime1 = rule.getPrime();
        int prime2 = rule.getPrime2();
        long slat = rule.getSlat();

        // 将字符还原成对应数字
        long[] a = new long[encodeLen];
        for (int i = 0; i < encodeLen; i++) {
            char c = encode.charAt(i);
            int index = findIndex(c, chars);
            if (index == -1) {
                // 异常字符串
                return null;
            }
            a[i * prime2 % encodeLen] = index;
        }

        long[] b = new long[encodeLen];
        for (int i = encodeLen - 2; i >= 0; i--) {
            b[i] = (a[i] - a[0] * i + charsLen * i) % charsLen;
        }

        long res = 0;
        for (int i = encodeLen - 2; i >= 0; i--) {
            res += b[i];
            res *= (i > 0 ? charsLen : 1);
        }
        return (res - slat) / prime1;
    }

    public static Long upperLimitId(Rule rule) {
        assert !RuleChanged(rule) : "规则已被修改";

        int charsLen = rule.getChars().length;
        int encodeLen = rule.getEncodeLen();
        int prime1 = rule.getPrime();
        long slat = rule.getSlat();

        return Math.round(Math.pow(charsLen, encodeLen - 1) - slat) / prime1 - 1;
    }

    private static int findIndex(char c, char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == c) {
                return i;
            }
        }
        return -1;
    }

    private static boolean RuleChanged(Rule rule) {
        return !rule.getCharsVer().equals(new String(rule.getChars()));
    }

    public static class Rule {

        /**
         * 随机字符串
         */
        private char[] chars;

        private String charsVer;

        /**
         * 随机码总长度长度
         */
        private int codeLen;

        /**
         * 加密校验码长度
         */
        private int encodeLen;

        /**
         * 随机数据
         */
        private long slat;

        /**
         * 前缀
         */
        private String prefix;

        /**
         * 后缀
         */
        private String suffix;

        private Pattern pattern;

        /**
         * 随机码填充率 1/times 校验码填充率 1 - 1/times
         */
        private int times = 3;

        public Rule(char[] chars, int codeLen, long slat) {
            this(chars, codeLen, slat, "", "");
        }

        public Rule(char[] chars, int codeLen, long slat, String prefix, String suffix) {
            assert !(Objects.isNull(chars) || codeLen <= 0 || slat <= 0) : "必填项错误";
            assert Math.round(Math.pow(chars.length, codeLen - 1) - slat) > 0 : "初始化随机值过大";
            assert (Objects.isNull(prefix) || Objects.isNull(suffix)
                    || codeLen >= prefix.length() + suffix.length()) : "前后缀长度>总长度";

            this.chars = chars;
            this.charsVer = new String(chars);
            this.codeLen = codeLen;
            int len = codeLen - prefix.length() - suffix.length();
            this.encodeLen = len - len / times;
            this.slat = slat;
            this.prefix = prefix;
            this.suffix = suffix;
            this.pattern = Pattern.compile("^" + prefix + "+(.*?)" + suffix + "$");
        }

        public String extractEncode(String code) {
            Matcher m = pattern.matcher(code);
            String temp = "";
            while (m.find()) {
                temp = m.group(1);
            }
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < temp.length(); i++) {
                if ((i + 1) % times != 0) {
                    buffer.append(temp.charAt(i));
                }
            }
            return buffer.toString();
        }

        public String insertEncode(int[] encodeIndex) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < encodeIndex.length; i++) {
                buffer.append(chars[encodeIndex[i]]);
                if ((buffer.length() + 1) % times == 0) {
                    buffer.append(chars[getRandomIndex()]);
                }
            }
            // if (!Objects.equals((codeLen - prefix.length() - suffix.length()) % times,
            // 0)) {
            // buffer.deleteCharAt(buffer.length() - 1);
            // }
            return prefix + buffer.toString() + suffix;
        }

        public char[] getChars() {
            return chars;
        }

        public String getCharsVer() {
            return charsVer;
        }

        public int getCodeLen() {
            return codeLen;
        }

        public int getEncodeLen() {
            return encodeLen;
        }

        public long getSlat() {
            return slat;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        private int getRandomIndex() {
            Random random = new Random();
            return random.nextInt(chars.length - 1);
        }

        /**
         * PRIME1 与 chars.length 互质，可保证 ( id * PRIME1) % chars.length 在
         * [0,chars.length)上均匀分布
         */
        public int getPrime() {
            int i = 3;
            for (; chars.length % i == 0; i++) {
            }
            return i;
        }

        /**
         * PRIME2 与 codeLen 互质，可保证 ( index * PRIME2) % codeLen 在 [0，codeLen）上均匀分布
         */
        public int getPrime2() {
            return 2 * encodeLen - 1;
        }

    }

}
