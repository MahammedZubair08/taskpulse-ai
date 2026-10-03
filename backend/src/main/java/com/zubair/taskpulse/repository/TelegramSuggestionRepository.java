package com.zubair.taskpulse.repository;
import com.zubair.taskpulse.entity.TelegramSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TelegramSuggestionRepository
        extends JpaRepository<TelegramSuggestion, Long> {
    Optional<TelegramSuggestion>
    findFirstByTelegramChatIdAndStatusOrderByCreatedAtDesc(
            Long telegramChatId,
            TelegramSuggestion.Status status
    );
}
