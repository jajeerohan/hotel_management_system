package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.RoomDto;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.exception.UnAuthorisedException;
import com.codingshuttle.projects.airBnbApp.repository.HotelRepository;
import com.codingshuttle.projects.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.codingshuttle.projects.airBnbApp.util.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    @Override
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating new room {} for the hotelId {}", roomDto.getId(), hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id " + hotelId));

        //This Login checks if the logged in user is owner of this hotel
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
        }
        Room room = modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        room = roomRepository.save(room);
        //TODO: Create future Inventory as soon as room is created and hotel is active
        if (hotel.getActive()) {
            inventoryService.initializeRoomForAYear(room);
        }
        return modelMapper.map(room, RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in the hotel {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id " + hotelId));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
        }
        List<RoomDto> roomList = hotel.getRooms().stream()
                .map(room -> modelMapper.map(room, RoomDto.class))
                .toList();
        return roomList;
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting the room with ID: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: "+roomId));
        return modelMapper.map(room, RoomDto.class);
    }

    @Transactional
    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Deleting the room with ID: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: "+roomId));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(room.getHotel().getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+room.getHotel().getId());
        }
        //Deleting All Inventory for this room as the room is getting deleted
        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(roomId);
    }

    @Transactional
    @Override
    public RoomDto updateRoomById(Long hotelId, Long roomId, RoomDto roomDto) {
        log.info("Updating the room with ID: {}", roomId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: "+hotelId));

        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: "+roomId));
        modelMapper.map(roomDto, room);
        room.setId(roomId);

        // TODO: if price or inventory is updated, then update the inventory for this room
        room = roomRepository.save(room);

        return modelMapper.map(room, RoomDto.class);
    }
}
