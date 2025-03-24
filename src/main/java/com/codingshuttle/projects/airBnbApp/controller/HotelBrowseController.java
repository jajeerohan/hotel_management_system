package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.*;
import com.codingshuttle.projects.airBnbApp.service.HotelService;
import com.codingshuttle.projects.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hotels")
public class HotelBrowseController {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
    /*public ResponseEntity<Page<HotelDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {
        Page<HotelDto> searchedResult = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(searchedResult);
    }*/
    public ResponseEntity<Page<HotelPriceResponseDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {
        var searchedResult = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(searchedResult);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfoById(@PathVariable Long hotelId, @RequestBody HotelInfoRequestDto hotelInfoRequestDto) {
        HotelInfoDto hotelInfoDto = hotelService.getHotelInfoById(hotelId, hotelInfoRequestDto);
        return ResponseEntity.ok(hotelInfoDto);
    }

}
