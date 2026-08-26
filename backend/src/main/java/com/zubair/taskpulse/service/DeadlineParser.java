package com.zubair.taskpulse.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeadlineParser {

    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    private static final Pattern IN_DAYS_PATTERN =
            Pattern.compile("in\\s+(\\d+)\\s+days?");

    private static final Pattern TIME_PATTERN =
            Pattern.compile(
                    "(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
                    Pattern.CASE_INSENSITIVE
            );

    public LocalDateTime parse(String expression) {

        if (expression == null || expression.isBlank()) {
            return null;
        }

        String value = expression
                .toLowerCase(Locale.ENGLISH)
                .trim();

        // Remove common deadline prefixes.
        value = value
                .replaceFirst("^by\\s+", "")
                .replaceFirst("^before\\s+", "")
                .trim();

        // Extract time before processing the date.
        LocalTime time = extractTime(value);

        // Remove time expression from date expression.
        String dateExpression = removeTime(value).trim();

        LocalDate date = parseDate(dateExpression);

        if (date == null) {
            return null;
        }

        return date.atTime(
                time != null ? time : END_OF_DAY
        );
    }

    private LocalDate parseDate(String expression) {

        LocalDate today = LocalDate.now();

        // today
        if (expression.equals("today")) {
            return today;
        }

        // tomorrow
        if (expression.equals("tomorrow")) {
            return today.plusDays(1);
        }

        // yesterday is invalid as a future deadline
        if (expression.equals("yesterday")) {
            return null;
        }

        // in X days
        Matcher daysMatcher =
                IN_DAYS_PATTERN.matcher(expression);

        if (daysMatcher.find()) {

            int days =
                    Integer.parseInt(daysMatcher.group(1));

            if (days < 0) {
                return null;
            }

            return today.plusDays(days);
        }

        // Weekdays
        DayOfWeek dayOfWeek =
                parseDayOfWeek(expression);

        if (dayOfWeek != null) {
            return nextOccurrence(today, dayOfWeek);
        }

        // Explicit ISO date: 2026-08-25
        try {
            return LocalDate.parse(
                    expression,
                    DateTimeFormatter.ISO_LOCAL_DATE
            );
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private DayOfWeek parseDayOfWeek(String expression) {

        return switch (expression) {

            case "monday", "next monday" ->
                    DayOfWeek.MONDAY;

            case "tuesday", "next tuesday" ->
                    DayOfWeek.TUESDAY;

            case "wednesday", "next wednesday" ->
                    DayOfWeek.WEDNESDAY;

            case "thursday", "next thursday" ->
                    DayOfWeek.THURSDAY;

            case "friday", "next friday" ->
                    DayOfWeek.FRIDAY;

            case "saturday", "next saturday" ->
                    DayOfWeek.SATURDAY;

            case "sunday", "next sunday" ->
                    DayOfWeek.SUNDAY;

            default -> null;
        };
    }

    private LocalDate nextOccurrence(
            LocalDate today,
            DayOfWeek target
    ) {

        int daysUntil =
                (target.getValue()
                        - today.getDayOfWeek().getValue()
                        + 7) % 7;

        // If today is the target day, interpret it as next occurrence.
        if (daysUntil == 0) {
            daysUntil = 7;
        }

        return today.plusDays(daysUntil);
    }

    private LocalTime extractTime(String expression) {

        Matcher matcher =
                TIME_PATTERN.matcher(expression);

        LocalTime result = null;

        while (matcher.find()) {

            int hour =
                    Integer.parseInt(matcher.group(1));

            String minuteGroup =
                    matcher.group(2);

            int minute =
                    minuteGroup != null
                            ? Integer.parseInt(minuteGroup)
                            : 0;

            String meridiem =
                    matcher.group(3);

            if (meridiem != null) {

                if (meridiem.equalsIgnoreCase("pm")
                        && hour < 12) {
                    hour += 12;
                }

                if (meridiem.equalsIgnoreCase("am")
                        && hour == 12) {
                    hour = 0;
                }
            }

            if (hour <= 23 && minute <= 59) {
                result = LocalTime.of(hour, minute);
            }
        }

        return result;
    }

    private String removeTime(String expression) {

        Matcher matcher =
                TIME_PATTERN.matcher(expression);

        return matcher.replaceAll("").trim();
    }
}