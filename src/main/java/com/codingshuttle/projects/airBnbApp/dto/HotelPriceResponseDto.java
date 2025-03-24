package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.HotelContactInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceResponseDto {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private HotelContactInfo contactInfo;
    private BigDecimal price;
}
