package com.zubair.taskpulse.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "telegram_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long telegramChatId;
    @Column(nullable = false)
    private String userEmail;
    @Column(nullable = false, length = 500)
    private String originalMessage;
    @Column(nullable = false, length = 255)
    private String title;
    private String priority;
    private String deadlineExpression;
    private LocalDateTime deadline;
    private Integer estimatedDuration;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    public enum Status {
        PENDING,
        CONFIRMED,
        DISMISSED
    }
    public Integer getEstimatedDurationM() {
        return estimatedDuration;
    }  
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
     
}
