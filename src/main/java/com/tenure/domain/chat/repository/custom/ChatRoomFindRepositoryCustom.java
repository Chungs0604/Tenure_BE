package com.tenure.domain.chat.repository.custom;

import com.tenure.domain.chat.entity.ChatRoomMember;
import com.tenure.domain.chat.enums.ChatRoomFilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface ChatRoomFindRepositoryCustom {

    Slice<ChatRoomMember> findChatRooms(
            Long userId, ChatRoomFilterType type,
            LocalDateTime cursor, LocalDateTime createdAtCursor,
            Long cursorId, Pageable pageable
    );
}
