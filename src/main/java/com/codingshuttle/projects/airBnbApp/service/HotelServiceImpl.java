package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.HotelDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelInfoDto;
import com.codingshuttle.projects.airBnbApp.dto.RoomDto;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.exception.UnAuthorisedException;
import com.codingshuttle.projects.airBnbApp.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    public final HotelRepository hotelRepository;
    public final ModelMapper modelMapper;
    public final InventoryService inventoryService;
    public final RoomService roomService;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new Hotel with name {}", hotelDto.getName());
        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel.setOwner(user);
        hotel = hotelRepository.save(hotel);
        log.info("Creating a new Hotel with id {} and name {}", hotel.getId(),hotel.getName());
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting the Hotel with id {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Hotel not found with id " + id));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+id);
        }
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Updating the Hotel with id {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Hotel not found with id " + id));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+id);
        }
        modelMapper.map(hotelDto, hotel);
        hotel.setId(id);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    @Transactional
    public Boolean deleteHotelById(Long id) {
        log.info("Delete the Hotel with id {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id " + id));

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+id);
        }
        //TODO: delete the future inventories for this hotel
        for (Room room : hotel.getRooms()) {
            roomService.deleteRoomById(room.getId()); //This will delete room as well as all the inventories of that room
        }
        hotelRepository.deleteById(id);
        return true;
    }

    @Override
    public void activateHotelById(Long id) {
        log.info("Activating the Hotel with id {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id " + id));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+id);
        }
        hotel.setActive(true);
        hotelRepository.save(hotel);

        //Creating Inventories once the hotel is Active
        for(Room room : hotel.getRooms()) {
            inventoryService.initializeRoomForAYear(room);
        }
    }

    // Public Method - visible to all
    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        log.info("Getting Hotel Info using hotelId {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id " + hotelId));
        List<RoomDto> roomDtoList = hotel.getRooms()
                .stream()
                .map(room -> modelMapper.map(room, RoomDto.class)).toList();
        return new HotelInfoDto(modelMapper.map(hotel, HotelDto.class), roomDtoList);
    }
}
