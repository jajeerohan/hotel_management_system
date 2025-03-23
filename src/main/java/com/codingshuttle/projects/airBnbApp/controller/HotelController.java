package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.HotelDto;
import com.codingshuttle.projects.airBnbApp.service.HotelService;
import com.codingshuttle.projects.airBnbApp.service.PricingUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {

    public final HotelService hotelService;
    public final PricingUpdateService pricingUpdateService;

    @PostMapping
    public ResponseEntity<HotelDto> createNewHotel(@RequestBody HotelDto hotelDto) {
        log.info("Controller Attempting to create a new hotel with name: {}", hotelDto.getName());
        HotelDto savedHotel = hotelService.createNewHotel(hotelDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHotel);
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long hotelId) {
        log.info("Controller Attempting to get hotel with id: {}", hotelId);
        HotelDto hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDto> updateHotelById(@PathVariable Long hotelId, @RequestBody HotelDto hotelDto) {
        log.info("Controller Attempting to update hotel with id: {}", hotelId);
        HotelDto updatedHotelDto = hotelService.updateHotelById(hotelId, hotelDto);
        return ResponseEntity.ok(updatedHotelDto);
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId) {
        log.info("Controller attempting to delete hotel with id: {}", hotelId);
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{hotelId}")
    public ResponseEntity<Void> activateHotel(@PathVariable Long hotelId) {
        log.info("Controller attempting to partially update hotel with id: {}", hotelId);
        hotelService.activateHotelById(hotelId);
//        pricingUpdateService.updatePrices();
        return ResponseEntity.noContent().build();
    }
}
