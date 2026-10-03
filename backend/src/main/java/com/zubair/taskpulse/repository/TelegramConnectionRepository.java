package com.zubair.taskpulse.repository;
import com.zubair.taskpulse.entity.TelegramConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TelegramConnectionRepository
        extends JpaRepository<TelegramConnection, Long> {
    Optional<TelegramConnection> findByTelegramChatId(Long telegramChatId);
    Optional<TelegramConnection> findByUserEmail(String userEmail);
}
