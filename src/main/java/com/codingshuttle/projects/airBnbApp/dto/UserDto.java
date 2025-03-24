package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserDto {

    private Long id;
    private String email;
    private String name;
    private Gender gender;
    private LocalDate dateOfBirth;
}
