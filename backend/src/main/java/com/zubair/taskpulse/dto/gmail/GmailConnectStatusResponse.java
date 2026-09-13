package com.zubair.taskpulse.dto.gmail;

public record GmailConnectStatusResponse(
        boolean connected,
        String email
) {}
