package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.HotelDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelInfoDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelPriceDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelSearchRequest;
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
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {
        Page<HotelPriceDto> searchedResult = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(searchedResult);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfoById(@PathVariable Long hotelId) {
        HotelInfoDto hotelInfoDto = hotelService.getHotelInfoById(hotelId);
        return ResponseEntity.ok(hotelInfoDto);
    }

}
