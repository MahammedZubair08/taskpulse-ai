package com.zubair.taskpulse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AIRateLimitService {

    private static final int MAX_REQUESTS = 20;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(String userEmail) {

        String key = "ai:rate:" + userEmail;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        return count != null && count <= MAX_REQUESTS;
    }
}