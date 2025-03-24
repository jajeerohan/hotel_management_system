package com.codingshuttle.projects.airBnbApp.repository;

import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByOwner(User user);
}
