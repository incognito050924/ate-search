package com.richslide.atesearch.business.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

public class DateTimeUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd a KK:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static Date parseDate(final String date) {
        Date result = null;
        try {
            final String dateString =  Objects.requireNonNull(date);

            String str = dateString.trim()
                    .replaceAll("오전", "AM")
                    .replaceAll("오후", "PM")
                    .replace("PM 12:", "AM 12:");

            if (str.split("M")[1].split(":")[0].trim().length() == 1)
                str = str.substring(0, 14) + "0" + str.substring(14);
            LocalDateTime ldt = DATE_TIME_FORMATTER.parse(str).query(LocalDateTime::from);
            result = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        return result;
    }
    public static LocalDate parseToLocalDate(final String dateString) {
        return LocalDate.parse("2017-02-23", DATE_FORMATTER);
    }

    public static String formatDateString(final String date) {
        return null;
    }

    public static String formatDateTimeString(final String datetime) {
        return null;
    }
}
