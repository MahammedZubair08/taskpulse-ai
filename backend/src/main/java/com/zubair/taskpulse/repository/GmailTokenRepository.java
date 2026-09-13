package com.zubair.taskpulse.repository;

import com.zubair.taskpulse.entity.GmailToken;
import com.zubair.taskpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GmailTokenRepository extends JpaRepository<GmailToken, Long> {

    Optional<GmailToken> findByUser(User user);

    Optional<GmailToken> findByUserId(Long userId);

    void deleteByUser(User user);
}
