package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.ProfileUpdateRequestDto;
import com.codingshuttle.projects.airBnbApp.dto.UserDto;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.codingshuttle.projects.airBnbApp.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {

    public final UserRepository userRepository;
    public final ModelMapper modelMapper;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User is not present with id " + id));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(null);
    }

    @Override
    public void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto) {
        User user = getCurrentUser();

        if(profileUpdateRequestDto.getDateOfBirth() != null) user.setDateOfBirth(profileUpdateRequestDto.getDateOfBirth());
        if(profileUpdateRequestDto.getGender() != null) user.setGender(profileUpdateRequestDto.getGender());
        if (profileUpdateRequestDto.getName() != null) user.setName(profileUpdateRequestDto.getName());

        userRepository.save(user);
    }

    @Override
    public UserDto getMyProfile() {
        User user = getCurrentUser();
        log.info("Getting the profile for user with id: {}", user.getId());
        return modelMapper.map(user, UserDto.class);
    }
}
