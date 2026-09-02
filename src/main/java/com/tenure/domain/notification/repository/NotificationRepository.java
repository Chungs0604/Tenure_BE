package com.tenure.domain.notification.repository;

import com.tenure.domain.notification.entity.Notification;
import com.tenure.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationSearchRepositoryCustom  {


    // 전체 읽음 수정(안읽은 알림 일괄 업데이트)
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now " +
            "where n.receiver.id = :currentUserId and n.readAt is null")
    int markAllRead(@Param("currentUserId") Long currentUserId, @Param("now") LocalDateTime now);

    // 채팅 알림: 채팅방에 CHAT_MESSAGE_CREATED인 알림 조회
    Optional<Notification> findByReceiverIdAndTargetIdAndType(
            Long receiverId, Long chatRoomId, NotificationType type);
}
