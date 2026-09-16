package com.sjbb.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DateUtil {

    public static final DateTimeFormatter LOCAL_TIME_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static final String YYYYMMDD = "yyyyMMdd";

    public static final String HH_MM_SS = "HH:mm:ss";

    public static final String HH_MM = "HH:mm";

    private static ThreadLocal<Map<String, SimpleDateFormat>> threadLocal = new ThreadLocal<>();

    /**
     * 返回当前线程内的sdfMap
     *
     * @param pattern
     * @return
     */
    public static SimpleDateFormat getSyncSdf(final String pattern) {
        Map<String, SimpleDateFormat> map = threadLocal.get();
        if (map == null) {
            map = new HashMap<>();
            map.put(pattern, new SimpleDateFormat(pattern));
            threadLocal.set(map);
        } else {
            if (!map.containsKey(pattern)) {
                map.put(pattern, new SimpleDateFormat(pattern));
            }
        }
        return map.get(pattern);
    }


    public static Date parse(String dateStr, String pattern) throws ParseException {

        return getSyncSdf(pattern).parse(dateStr);
    }

    /**
     * 据传递的格式获取当前时间
     *
     * @param pattern
     * @return
     */
    public static String getCurrentTime(String pattern) {

        return getSyncSdf(pattern).format(new Date());
    }

    /**
     * 根据传递的格式返回字符串对应日期
     *
     * @param str
     * @param pattern
     * @return
     * @throws ParseException
     */
    public static Date stringToDate(String str, String pattern) throws ParseException {
        return getSyncSdf(pattern).parse(str);
    }

    /**
     * @param date
     * @param pattern
     * @return
     */
    public static String dateToString(Date date, String pattern) {
        if (null == date) {
            return null;
        }
        return getSyncSdf(pattern).format(date);
    }

    /**
     * date 格式化
     *
     * @param date
     * @param pattern
     * @return
     * @throws ParseException
     */
    public static Date formatDate(Date date, String pattern) throws ParseException {
        return stringToDate(dateToString(date, pattern), pattern);
    }

    /**
     * 当前日期
     *
     * @param pattern
     * @return
     */
    public static String getToday(String pattern) {
        return getSyncSdf(pattern).format(new Date());
    }

    /**
     * 获取昨天 Day-1
     *
     * @param pattern
     * @return
     */
    public static String getYesterday(String pattern) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        return getSyncSdf(pattern).format(calendar.getTime());
    }

    public static Date getSysYesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        return calendar.getTime();
    }

    /**
     * 以当前日期为起点的偏移
     *
     * @param offset
     * @param pattern
     * @return
     */
    public static String getSysOffsetDay(int offset, String pattern) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, offset);
        return getSyncSdf(pattern).format(cal.getTime());
    }

    /**
     * 以传入的日期为起点的偏移
     *
     * @param date
     * @param offset
     * @return
     */
    public static String getDayByNum(Date date, int offset) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, offset);
        return DateUtil.dateToString(cal.getTime(), DateUtil.YYYY_MM_DD);
    }


    /**
     * 以传入的日期为起点的偏移(小时)
     *
     * @param date
     * @param offset
     * @return
     */
    public static Date getHourByNum(Date date, int offset) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, offset);
        return cal.getTime();
    }


    /**
     * 上一周的周一
     *
     * @param pattern
     * @return
     */
    public static String getLastWeekFirstDay(String pattern) {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(Calendar.DAY_OF_WEEK, -14);
        } else {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(Calendar.DAY_OF_WEEK, -7);
        }
        return getSyncSdf(pattern).format(cal.getTime());
    }


    /**
     * 当前周的周一
     *
     * @param pattern
     * @return
     */
    public static String getCurrWeekFirstDay(String pattern) {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(Calendar.DAY_OF_WEEK, -7);
        } else {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }
        return getSyncSdf(pattern).format(cal.getTime());
    }

    /**
     * 下一周的周一
     *
     * @param pattern
     * @return
     */
    public static String getNextWeekFirstDay(String pattern) {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        } else {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(Calendar.DAY_OF_WEEK, 7);
        }
        return getSyncSdf(pattern).format(cal.getTime());
    }

    /**
     * 上个月的第一天
     *
     * @param pattern
     * @return
     */
    public static String getLastMonthFirstDay(String pattern) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -1);
        return getSyncSdf(pattern).format(cal.getTime());
    }

    /**
     * 当前月的第一天
     *
     * @param pattern
     * @return
     */
    public static String getCurrMonthFirstDay(String pattern) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);//设置为1号,当前日期既为本月第一天
        return getSyncSdf(pattern).format(cal.getTime());
    }

    /**
     * 下个月的第一天
     *
     * @param pattern
     * @return
     */
    public static String getNextMonthFirstDay(String pattern) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        return getSyncSdf(pattern).format(cal.getTime());
    }


    /**
     * 相差的天数
     *
     * @param d1
     * @param d2
     * @return
     */
    public static int getDiffDayAbs(Date d1, Date d2) {
        int days = (int) ((d1.getTime() - d2.getTime()) / (1000 * 3600 * 24));
        return Math.abs(days);
    }

    /**
     * 相差的分钟数
     *
     * @param d1
     * @param d2
     * @return
     */
    public static int getDiffMinuteAbs(Date d1, Date d2) {
        int diff = (int) ((d1.getTime() - d2.getTime()) / (1000 * 60));
        return Math.abs(diff);
    }

    /**
     * 若d1 或d2 为null,则返回false
     *
     * @param d1
     * @param d2
     * @param pattern
     * @return
     */
    public static boolean isSameTime(Date d1, Date d2, String pattern) {
        if (null == d1 || null == d2) {
            return false;
        }
        return dateToString(d1, pattern).equals(dateToString(d2, pattern));
    }

}
