package com.game.monopoly.controller;

import com.game.monopoly.dto.*;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.enums.RoomVisibility;
import com.game.monopoly.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public RoomListResponse getRooms(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) RoomVisibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return roomService.getRooms(status, visibility, page, size);
    }

    @PostMapping
    public RoomCreateResponse createRoom(@RequestBody RoomCreateRequest request, Authentication authentication) {
        return roomService.createRoom(request, authentication);
    }

    @PostMapping("/join")
    public RoomJoinResponse joinRoom(@RequestBody RoomJoinRequest request, Authentication authentication) {
        return roomService.joinRoom(request, authentication);
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse getRoomDetail(@PathVariable Long roomId, Authentication authentication) {
        return roomService.getRoomDetail(roomId, authentication);
    }

    @PatchMapping("/{roomId}/settings")
    public MessageResponse updateRoomSettings(
            @PathVariable Long roomId,
            @RequestBody RoomSettingsUpdateRequest request,
            Authentication authentication
    ) {
        return roomService.updateRoomSettings(roomId, request, authentication);
    }

    @PostMapping("/{roomId}/hero")
    public RoomHeroSelectResponse selectHero(
            @PathVariable Long roomId,
            @RequestBody RoomHeroSelectRequest request,
            Authentication authentication
    ) {
        return roomService.selectHero(roomId, request, authentication);
    }

    @PostMapping("/{roomId}/ready")
    public RoomReadyResponse setReady(
            @PathVariable Long roomId,
            @RequestBody RoomReadyRequest request,
            Authentication authentication
    ) {
        return roomService.setReady(roomId, request, authentication);
    }

    @PostMapping("/{roomId}/start")
    public RoomStartResponse startGame(@PathVariable Long roomId, Authentication authentication) {
        return roomService.startGame(roomId, authentication);
    }

    @PostMapping("/{roomId}/leave")
    public MessageResponse leaveRoom(@PathVariable Long roomId, Authentication authentication) {
        return roomService.leaveRoom(roomId, authentication);
    }
}
