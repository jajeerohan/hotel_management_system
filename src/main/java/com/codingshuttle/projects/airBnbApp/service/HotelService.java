package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.HotelDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelInfoDto;
import com.codingshuttle.projects.airBnbApp.dto.HotelInfoRequestDto;

import java.util.List;

public interface    HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

    HotelDto updateHotelById(Long id, HotelDto hotelDto);

    Boolean deleteHotelById(Long id);

    void activateHotelById(Long id);

    HotelInfoDto getHotelInfoById(Long hotelId, HotelInfoRequestDto hotelInfoRequestDto);

    List<HotelDto> getAllHotels();
}


