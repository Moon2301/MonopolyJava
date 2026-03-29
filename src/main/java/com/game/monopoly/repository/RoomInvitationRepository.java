package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.RoomInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {
    List<RoomInvitation> findByInviteeIdAndStatus(Long inviteeId, String status);
    Optional<RoomInvitation> findByRoomIdAndInviteeIdAndStatus(Long roomId, Long inviteeId, String status);
}
