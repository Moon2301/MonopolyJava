package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByRequesterIdOrReceiverId(Long requesterId, Long receiverId);
    Optional<Friendship> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);
    int countByReceiverIdAndStatus(Long receiverId, String status);
}
