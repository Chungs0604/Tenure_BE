package com.tenure.domain.chat.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tenure.domain.chat.entity.ChatRoomMember;
import com.tenure.domain.chat.enums.ChatRoomFilterType;
import com.tenure.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static com.tenure.domain.chat.entity.QChatRoom.chatRoom;
import static com.tenure.domain.chat.entity.QChatRoomMember.chatRoomMember;
import static com.tenure.domain.item.entity.QItem.item;
import static com.tenure.domain.user.entity.QUserBlock.userBlock;

@RequiredArgsConstructor
public class ChatRoomFindRepositoryCustomImpl implements ChatRoomFindRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    // 카테고리 별 채틴방 조회
    @Override
    public Slice<ChatRoomMember> findChatRooms(
            Long userId, ChatRoomFilterType type, LocalDateTime cursor,
            LocalDateTime createdAtCursor, Long cursorId, Pageable pageable
    ) {

        QUser buyer = new QUser("buyer");
        QUser seller = new QUser("seller");

        int pageSize = pageable.getPageSize();

        List<ChatRoomMember> content = queryFactory
                .select(chatRoomMember)
                .from(chatRoomMember)
                .join(chatRoomMember.chatRoom, chatRoom).fetchJoin()
                .join(chatRoom.seller, seller).fetchJoin()
                .join(chatRoom.buyer, buyer).fetchJoin()
                .join(chatRoom.item, item).fetchJoin()
                .where(
                        chatRoomMember.user.id.eq(userId),
                        chatRoomMember.isExited.eq(false),
                        filterChatRoomType(userId, type),
                        isBlocked(userId),
                        cursorCondition(cursor, createdAtCursor, cursorId)
                )
                .orderBy(chatRoom.lastMessageAt.desc().nullsLast(), chatRoom.createdAt.desc(), chatRoom.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageSize) {
            content.remove(pageSize);
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression cursorCondition(
            LocalDateTime cursor, LocalDateTime createdAtCursor, Long cursorId
    ) {
        // 최초 요청: 맨 처음 페이지
        if(cursor == null || createdAtCursor == null || cursorId == null) {
            return null;
        }

        // 채팅방에 메시지가 없는 경우 채팅방 생성 시간을 기점으로 커서
        BooleanExpression noneLastMessageCondition = chatRoom.lastMessageAt.isNull()
                .and(
                        chatRoom.createdAt.lt(createdAtCursor)
                                .or(chatRoom.createdAt.eq(createdAtCursor).and(chatRoom.id.lt(cursorId)))
                );

        // 채팅방에 메시지가 있을 경우 마지막 메시지 시간을 기점으로 커서
        BooleanExpression lastMessageCondition = chatRoom.lastMessageAt.lt(cursor)
                .or(chatRoom.lastMessageAt.eq(cursor).and(chatRoom.id.lt(cursorId)));

        return noneLastMessageCondition.or(lastMessageCondition);
    }


    private BooleanExpression isBlocked(Long userId) {

        // 채팅방의 상대방이 내가 차단한 유저가 아니어야 함

        return  JPAExpressions
                        .select(userBlock)
                        .from(userBlock)
                        .where(
                                userBlock.blocker.id.eq(userId),
                                // 내가 구매자면 판매자가 차단되었는지, 내가 판매자면 구매자가 차단되었는지 검사
                                (chatRoom.buyer.id.eq(userId).and(userBlock.blocked.id.eq(chatRoom.seller.id)))
                                        .or(chatRoom.seller.id.eq(userId).and(userBlock.blocked.id.eq(chatRoom.buyer.id)))
                        )
                        .notExists();
    }

    private BooleanExpression filterChatRoomType(Long userId, ChatRoomFilterType type) {
        if (type == null) return null;

        // 채팅방 타입별 분기
        return switch (type) {
            case BUYING ->  chatRoom.buyer.id.eq(userId);
            case SELLING -> chatRoom.seller.id.eq(userId);
            case UNREAD -> chatRoomMember.unreadCount.gt(0);
            case ALL-> null;
        };

    }
}
