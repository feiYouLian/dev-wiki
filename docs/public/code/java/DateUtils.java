package com.sjbb;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 *
 * @Description: 线程安全的时间工具类
 * @Author: youlian.fei
 * @Date: 18:43 2019/1/24
 */
public class DateUtils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER_YYYY_MM_DD_00_00_00 = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");
    public static final DateTimeFormatter DATE_FORMATTER_YYYY_MM_DD_23_59_59 = DateTimeFormatter.ofPattern("yyyy-MM-dd 23:59:59");
    public static final DateTimeFormatter DATE_FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter DATE_FORMATTER_MM_DD = DateTimeFormatter.ofPattern("MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    //一周计算方法  周一为一周的第一天，一周有7天
    public static final TemporalField WEEKFIELD = WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek();

    //默认时区
    public static final ZoneId zoneId = ZoneId.systemDefault();

    public static final Integer ZERO = 0;

    private static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(zoneId).toInstant());
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();
    }

    private static String toString(LocalDateTime localDateTime, DateTimeFormatter dtf) {
        return localDateTime.format(dtf);
    }

    /**
     * @param offset 偏移量
     * @param unit   单位 ChronoUnit
     * @return Date
     */
    public static Date offset(long offset, TemporalUnit unit) {
        return toDate(LocalDateTime.now().plus(offset, unit));
    }

    /**
     * @param date   时间
     * @param offset 偏移量
     * @param unit   单位 ChronoUnit
     * @return Date
     */
    public static Date offset(Date date, long offset, TemporalUnit unit) {
        return toDate(toLocalDateTime(date).plus(offset, unit));
    }

    /**
     * str 只能是年月日，不可以带时分秒。eq: yyyyMMdd ,yyyy-MM-dd 等
     *
     * @param str 时间字符串
     * @param dtf 格式
     * @return Date
     */
    public static Date toDate(String str, DateTimeFormatter dtf) {
        return toDate(LocalDateTime.of(LocalDate.parse(str, dtf), LocalTime.of(ZERO, ZERO)));
    }

    /**
     * str 是年月日时分秒。eq: yyyy-MM-dd HH:mm:ss 等
     *
     * @param str 时间字符串
     * @param dtf 格式
     * @return Date
     */
    public static Date toDateTime(String str, DateTimeFormatter dtf) {
        return toDate(LocalDateTime.parse(str, dtf));
    }

    /**
     * @param date 时间
     * @param dtf  格式
     * @return String
     */
    public static String toString(Date date, DateTimeFormatter dtf) {
        return toLocalDateTime(date).format(dtf);
    }

    /**
     * @param dtf 格式
     * @return String
     */
    public static String getCurrTime(DateTimeFormatter dtf) {
        return LocalDateTime.now().format(dtf);
    }


    /**
     * @param offset 偏移量
     * @param unit   单位 ChronoUnit
     * @param dtf    格式
     * @return string
     */
    public static String offsetAndToString(long offset, TemporalUnit unit, DateTimeFormatter dtf) {
        return toString(LocalDateTime.now().plus(offset, unit), dtf);
    }

    /**
     * @param date   时间
     * @param offset 偏移量
     * @param unit   单位 ChronoUnit
     * @param dtf    格式
     * @return string
     */
    public static String offsetAndToString(Date date, long offset, TemporalUnit unit, DateTimeFormatter dtf) {
        return toString(toLocalDateTime(date).plus(offset, unit), dtf);
    }

    /**
     * @param dayOfweek 1-7 表示 周一到周日
     * @param dtf       格式
     * @return String
     */
    public static String getCurrWeek(long dayOfweek, DateTimeFormatter dtf) {
        return toString(LocalDateTime.now().with(WEEKFIELD, dayOfweek), dtf);
    }

    /**
     * @param dayOfweek 1-7 表示 周一到周日
     * @param dtf       格式
     * @return String
     */
    public static String getNextWeek(long dayOfweek, DateTimeFormatter dtf) {
        return toString(LocalDateTime.now().plusWeeks(1).with(WEEKFIELD, dayOfweek), dtf);
    }

    /**
     * @param dayOfMonth 某月的第多少天
     * @param dtf        格式
     * @return String
     */
    public static String getCurrMonth(long dayOfMonth, DateTimeFormatter dtf) {
        return toString(LocalDateTime.now().with(ChronoField.DAY_OF_MONTH, dayOfMonth), dtf);
    }

    /**
     * @param dayOfMonth 某月的第多少天
     * @param dtf        格式
     * @return String
     */
    public static String getNextMonth(long dayOfMonth, DateTimeFormatter dtf) {
        return toString(LocalDateTime.now().plusMonths(1).with(ChronoField.DAY_OF_MONTH, dayOfMonth), dtf);
    }


    /**
     * 相差的天数
     *
     * @param d1 日期
     * @param d2 日期
     * @return int
     */
    public static int getMinusDays(Date d1, Date d2) {
        int days = (int) ((d1.getTime() - d2.getTime()) / (1000 * 3600 * 24));
        return days;
    }

    /**
     * 若d1 或d2 为null,则返回false
     *
     * @param d1  日期
     * @param d2  日期
     * @param dtf 格式
     * @return boolean
     */
    public static boolean isSameTime(Date d1, Date d2, DateTimeFormatter dtf) {
        if (null == d1 || null == d2) {
            return false;
        }
        return Objects.equals(toString(toLocalDateTime(d1), dtf), toString(toLocalDateTime(d2), dtf));
    }

}
