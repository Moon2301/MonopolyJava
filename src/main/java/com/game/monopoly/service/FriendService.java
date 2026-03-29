package com.game.monopoly.service;

import com.game.monopoly.dto.FriendRequestDto;
import com.game.monopoly.dto.FriendResponse;
import com.game.monopoly.dto.MessageResponse;
import com.game.monopoly.model.metaData.Friendship;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.FriendshipRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public MessageResponse sendFriendRequest(FriendRequestDto request, Long accountId) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException("Tên người dùng không được để trống");
        }

        UserProfile targetProfile = userProfileRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Long targetId = targetProfile.getAccount().getAccountId();
        if (Objects.equals(targetId, accountId)) {
            throw new RuntimeException("Không thể kết bạn với chính mình");
        }

        Optional<Friendship> existing1 = friendshipRepository.findByRequesterIdAndReceiverId(accountId, targetId);
        Optional<Friendship> existing2 = friendshipRepository.findByRequesterIdAndReceiverId(targetId, accountId);

        if (existing1.isPresent() || existing2.isPresent()) {
            throw new RuntimeException("Đã tồn tại trạng thái kết bạn/chờ kết bạn với người này");
        }

        friendshipRepository.save(Friendship.builder()
                .requesterId(accountId)
                .receiverId(targetId)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build());

        return MessageResponse.builder().message("Đã gửi lời mời kết bạn!").build();
    }

    @Transactional
    public MessageResponse acceptFriendRequest(Long friendshipId, Long accountId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu không tồn tại"));

        if (!Objects.equals(friendship.getReceiverId(), accountId)) {
            throw new RuntimeException("Bạn không có quyền chấp nhận yêu cầu này");
        }

        friendship.setStatus("ACCEPTED");
        friendshipRepository.save(friendship);

        return MessageResponse.builder().message("Đã chấp nhận kết bạn!").build();
    }

    @Transactional
    public MessageResponse declineOrRemoveFriend(Long friendshipId, Long accountId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mối quan hệ này"));

        if (!Objects.equals(friendship.getRequesterId(), accountId) && !Objects.equals(friendship.getReceiverId(), accountId)) {
            throw new RuntimeException("Không có quyền thực hiện");
        }

        friendshipRepository.delete(friendship);
        return MessageResponse.builder().message("Thành công").build();
    }

    @Transactional(readOnly = true)
    public FriendResponse getFriends(Long accountId) {
        List<Friendship> allRelations = friendshipRepository.findByRequesterIdOrReceiverId(accountId, accountId);
        
        List<FriendResponse.FriendDto> friends = new ArrayList<>();
        List<FriendResponse.FriendDto> pendingRequests = new ArrayList<>();

        for (Friendship f : allRelations) {
            Long otherId = Objects.equals(f.getRequesterId(), accountId) ? f.getReceiverId() : f.getRequesterId();
            UserProfile profile = userProfileRepository.findByAccount_AccountId(otherId).orElse(null);
            if (profile == null) continue;

            FriendResponse.FriendDto dto = FriendResponse.FriendDto.builder()
                    .friendshipId(f.getId())
                    .accountId(otherId)
                    .username(profile.getUsername())
                    .avatarUrl(profile.getAvatarUrl() != null && profile.getAvatarUrl().isBlank() ? null : profile.getAvatarUrl())
                    .build();

            if ("ACCEPTED".equals(f.getStatus())) {
                friends.add(dto);
            } else if ("PENDING".equals(f.getStatus())) {
                if (Objects.equals(f.getReceiverId(), accountId)) {
                    pendingRequests.add(dto);
                }
            }
        }

        return FriendResponse.builder()
                .friends(friends)
                .pendingRequests(pendingRequests)
                .build();
    }

    @Transactional(readOnly = true)
    public int getPendingCount(Long accountId) {
        return friendshipRepository.countByReceiverIdAndStatus(accountId, "PENDING");
    }
}
