package com.tenure.domain.notification.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tenure.domain.notification.entity.Notification;
import com.tenure.domain.notification.entity.QNotification;
import com.tenure.domain.notification.enums.NotificationCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static com.tenure.domain.notification.entity.QNotification.notification;

@RequiredArgsConstructor
public class NotificationSearchRepositoryCustomImpl implements NotificationSearchRepositoryCustom{

    private final JPAQueryFactory queryFactory;


    /**
     "select n from Notification n " +
     "where n.receiver.id = :receiver_id " +
     "and (:category is null or n.category = :category) " +
     "and (:unReadOnly = false or n.readAt is null) " +
     "and (n.createdAt < :cursor or (n.createdAt = :cursor and n.id < :cursorId))"
     */

    // 전체 알림 조회
    @Override
    public Slice<Notification> findAllNotification(
            Long receiverId, NotificationCategory category, boolean unReadOnly,
            LocalDateTime cursor, Long cursorId, Pageable pageable
    ) {

        int pageSize = pageable.getPageSize();

        List<Notification> content = queryFactory
                .select(notification)
                .from(notification)
                .where(
                        notification.receiver.id.eq(receiverId),
                        categoryEq(category),
                        isUnReadOnly(unReadOnly),
                        cursorCondition(cursor, cursorId)
                )
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = false;

        if(content.size() > pageSize) {
            content.remove(pageSize);
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression cursorCondition(LocalDateTime cursor, Long cursorId) {
        if (cursor == null || cursorId == null) return null;

        return notification.createdAt.lt(cursor)
                .or(notification.createdAt.eq(cursor).and(notification.id.lt(cursorId)));
    }

    private BooleanExpression isUnReadOnly(boolean unReadOnly) {
        return unReadOnly ? notification.readAt.isNull() : null;
    }

    private BooleanExpression categoryEq(NotificationCategory category) {
        return category != null ? notification.category.eq(category) : null;
    }
}
