package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GuestDto {

    private Long id;
    private String name;
    private Gender gender;
    private LocalDate dateOfBirth;

}
