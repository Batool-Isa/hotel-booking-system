package com.ga.hotelservice.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSummary {
    private Long id;
    private String email;
    private String role;
    private String status;
}
