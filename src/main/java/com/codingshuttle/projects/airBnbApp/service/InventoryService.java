package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.*;
import com.codingshuttle.projects.airBnbApp.entity.HotelMinPrice;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

//    Page<HotelDto> searchHotels(HotelSearchRequest hotelSearchRequest);
    Page<HotelPriceResponseDto> searchHotels(HotelSearchRequest hotelSearchRequest);

    List<InventoryDto> getAllInventoryByRoom(Long roomId);

    void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto);
}
