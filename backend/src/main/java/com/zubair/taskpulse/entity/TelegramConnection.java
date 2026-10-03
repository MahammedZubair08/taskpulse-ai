package com.zubair.taskpulse.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "telegram_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long telegramChatId;
    @Column(nullable = false)
    private String userEmail;
    private String telegramUsername;
    @Column(nullable = false)
    private LocalDateTime connectedAt;
    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
    }
}
