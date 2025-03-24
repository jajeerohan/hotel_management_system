package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomPriceDto {
    private Room room;
    private Double price;
}
