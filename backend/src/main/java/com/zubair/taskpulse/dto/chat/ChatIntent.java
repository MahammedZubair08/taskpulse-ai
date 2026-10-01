package com.zubair.taskpulse.dto.chat;
public record ChatIntent(
        IntentType intent,
        String query,
        String priority,
        String status
) {
    public enum IntentType {
        ALL_TASKS,
        DUE_TODAY,
        DUE_THIS_WEEK,
        OVERDUE,
        BY_PRIORITY,
        BY_STATUS,
        SEARCH,
        UNKNOWN
    }
}
