package com.game.monopoly.service;

import com.game.monopoly.dto.FriendListItemResponse;
import com.game.monopoly.dto.MessageItemResponse;
import com.game.monopoly.model.metaData.*;
import com.game.monopoly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final FriendRepository friendRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<FriendListItemResponse> getFriends(Long accountId) {
        UserProfile me = getCurrentProfile(accountId);
        List<Friend> relations = friendRepository.findAllByUserId(me.getUserProfileId());
        List<FriendListItemResponse> result = new ArrayList<>();

        for (Friend relation : relations) {
            UserProfile other = relation.getRequester().getUserProfileId().equals(me.getUserProfileId())
                    ? relation.getAddressee()
                    : relation.getRequester();

            String avatar = other.getAvatarUrl();
            if (avatar == null || avatar.isBlank()) {
                avatar = DEFAULT_AVATAR;
            }

            Long unread = messageRepository.countUnreadBySender(me.getUserProfileId(), other.getUserProfileId());
            boolean pendingOutgoing =
                    "PENDING".equals(relation.getStatus())
                            && relation.getRequester().getUserProfileId().equals(me.getUserProfileId());
            boolean canAccept =
                    "PENDING".equals(relation.getStatus())
                            && relation.getAddressee().getUserProfileId().equals(me.getUserProfileId());
            result.add(FriendListItemResponse.builder()
                    .friendId(relation.getFriendId())
                    .userProfileId(other.getUserProfileId())
                    .username(other.getUsername())
                    .avatarUrl(avatar)
                    .status(relation.getStatus())
                    .unreadMessages(unread)
                    .canAccept(canAccept)
                    .pendingOutgoing(pendingOutgoing)
                    .build());
        }

        return result;
    }

    @Transactional
    public FriendListItemResponse sendFriendRequest(Long accountId, String friendUsername) {
        UserProfile me = getCurrentProfile(accountId);
        if (friendUsername == null || friendUsername.isBlank()) {
            throw new RuntimeException("friendUsername is required");
        }

        UserProfile target = userProfileRepository.findByUsername(friendUsername.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (me.getUserProfileId().equals(target.getUserProfileId())) {
            throw new RuntimeException("Không thể kết bạn với chính mình");
        }

        Friend existing = friendRepository.findRelationBetween(me.getUserProfileId(), target.getUserProfileId())
                .orElse(null);
        if (existing != null) {
            throw new RuntimeException("Yêu cầu kết bạn đã tồn tại");
        }

        Friend friend = new Friend();
        friend.setRequester(me);
        friend.setAddressee(target);
        friend.setStatus("PENDING");
        friend = friendRepository.save(friend);

        String avatar = target.getAvatarUrl();
        if (avatar == null || avatar.isBlank()) {
            avatar = DEFAULT_AVATAR;
        }

        return FriendListItemResponse.builder()
                .friendId(friend.getFriendId())
                .userProfileId(target.getUserProfileId())
                .username(target.getUsername())
                .avatarUrl(avatar)
                .status(friend.getStatus())
                .unreadMessages(0L)
                .canAccept(false)
                .pendingOutgoing(true)
                .build();
    }

    @Transactional
    public FriendListItemResponse acceptFriendRequest(Long accountId, Long friendId) {
        UserProfile me = getCurrentProfile(accountId);
        Friend relation = friendRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!relation.getAddressee().getUserProfileId().equals(me.getUserProfileId())) {
            throw new RuntimeException("Bạn không có quyền chấp nhận yêu cầu này");
        }

        relation.setStatus("ACCEPTED");
        relation = friendRepository.save(relation);

        UserProfile other = relation.getRequester();
        String avatar = other.getAvatarUrl();
        if (avatar == null || avatar.isBlank()) {
            avatar = DEFAULT_AVATAR;
        }

        return FriendListItemResponse.builder()
                .friendId(relation.getFriendId())
                .userProfileId(other.getUserProfileId())
                .username(other.getUsername())
                .avatarUrl(avatar)
                .status(relation.getStatus())
                .unreadMessages(messageRepository.countUnreadBySender(me.getUserProfileId(), other.getUserProfileId()))
                .canAccept(false)
                .pendingOutgoing(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageItemResponse> getConversation(Long accountId, Long friendUserProfileId) {
        UserProfile me = getCurrentProfile(accountId);
        ensureAcceptedFriend(me.getUserProfileId(), friendUserProfileId);

        return messageRepository.findConversation(me.getUserProfileId(), friendUserProfileId).stream()
                .limit(100)
                .map(message -> MessageItemResponse.builder()
                        .messageId(message.getMessageId())
                        .senderId(message.getSender().getUserProfileId())
                        .receiverId(message.getReceiver().getUserProfileId())
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .isRead(message.getIsRead())
                        .build())
                .toList();
    }

    @Transactional
    public MessageItemResponse sendMessage(Long accountId, Long toUserProfileId, String content) {
        UserProfile me = getCurrentProfile(accountId);
        if (toUserProfileId == null) {
            throw new RuntimeException("toUserProfileId is required");
        }
        if (content == null || content.isBlank()) {
            throw new RuntimeException("content is required");
        }

        UserProfile receiver = userProfileRepository.findById(toUserProfileId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        ensureAcceptedFriend(me.getUserProfileId(), receiver.getUserProfileId());

        Message message = new Message();
        message.setSender(me);
        message.setReceiver(receiver);
        message.setContent(content.trim());
        message.setIsRead(false);
        message = messageRepository.save(message);

        return MessageItemResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserProfileId())
                .receiverId(message.getReceiver().getUserProfileId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                .isRead(message.getIsRead())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageItemResponse> getChannelMessages(Long accountId) {
        // Validate current user session
        getCurrentProfile(accountId);

        List<Message> messages = messageRepository.findTop50ByReceiverIsNullOrderByCreatedAtDesc();
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> MessageItemResponse.builder()
                        .messageId(message.getMessageId())
                        .senderId(message.getSender().getUserProfileId())
                        .receiverId(null)
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .isRead(message.getIsRead())
                        .build())
                .toList();
    }

    @Transactional
    public MessageItemResponse sendChannelMessage(Long accountId, String content) {
        UserProfile me = getCurrentProfile(accountId);
        if (content == null || content.isBlank()) {
            throw new RuntimeException("content is required");
        }

        Message message = new Message();
        message.setSender(me);
        message.setReceiver(null);
        message.setContent(content.trim());
        message.setIsRead(false);
        message = messageRepository.save(message);

        return MessageItemResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserProfileId())
                .receiverId(null)
                .content(message.getContent())
                .createdAt(message.getCreatedAt().format(DATE_TIME_FORMATTER))
                .isRead(message.getIsRead())
                .build();
    }

    private UserProfile getCurrentProfile(Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("Missing X-Account-Id header");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    private void ensureAcceptedFriend(Long meUserProfileId, Long otherUserProfileId) {
        Friend relation = friendRepository.findRelationBetween(meUserProfileId, otherUserProfileId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa kết bạn với người dùng này"));
        if (!"ACCEPTED".equalsIgnoreCase(relation.getStatus())) {
            throw new RuntimeException("Mối quan hệ bạn bè chưa được chấp nhận");
        }
    }
}
