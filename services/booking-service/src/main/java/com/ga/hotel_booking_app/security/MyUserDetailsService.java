package com.ga.hotel_booking_app.security;

import com.ga.hotel_booking_app.exception.custom.InvalidCredentialsException;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private UserService userService;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userService.findUserByEmailAddress(email);
        if(user == null){
            throw new UsernameNotFoundException("User with email "+email+" not found");
        }
        return new MyUserDetails(user);
    }
}
