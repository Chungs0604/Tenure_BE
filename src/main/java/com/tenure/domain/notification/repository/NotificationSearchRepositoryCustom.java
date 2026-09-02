package com.tenure.domain.notification.repository;

import com.tenure.domain.notification.entity.Notification;
import com.tenure.domain.notification.enums.NotificationCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface NotificationSearchRepositoryCustom {

    Slice<Notification> findAllNotification(
            Long receiverId,
            NotificationCategory category,
            boolean unReadOnly,
            LocalDateTime cursor,
            Long cursorId,
            Pageable pageable
    );
}
