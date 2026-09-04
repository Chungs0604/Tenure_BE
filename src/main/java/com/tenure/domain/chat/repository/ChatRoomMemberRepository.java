package com.tenure.domain.chat.repository;

import com.tenure.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    boolean existsByUserIdAndChatRoomIdAndIsExitedFalse(Long userId, Long chatRoomId);

    //해당 채팅방의 사용자 조회
    Optional<ChatRoomMember> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);
}
