package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.HotelContactInfo;
import lombok.Data;

@Data
public class HotelDto {

    private Long id;
    private String name;
    private String city;
    private String[] photos; // urls of photos
    private String[] amenities;
    private HotelContactInfo contactInfo;
    private Boolean active;

}
