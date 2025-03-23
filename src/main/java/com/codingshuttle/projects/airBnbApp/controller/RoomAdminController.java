package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.RoomDto;
import com.codingshuttle.projects.airBnbApp.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/hotels/{hotelId}/rooms")
public class RoomAdminController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDto> createNewRoom(@PathVariable Long hotelId,
                                                 @RequestBody RoomDto roomDto) {
        log.info("Room Controller creating new room for hotel {}", hotelId);
        RoomDto savedRoomDto = roomService.createNewRoom(hotelId, roomDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoomDto);
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllRoomsInHotel(@PathVariable Long hotelId) {
        log.info("Room Controller getting all room for hotel {}", hotelId);
        return new ResponseEntity<>(roomService.getAllRoomsInHotel(hotelId), HttpStatus.OK);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        log.info("Room Controller getting room by id {}", roomId);
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<RoomDto> deleteRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        log.info("Room Controller deleting room by roomId {}", roomId);
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }

}
