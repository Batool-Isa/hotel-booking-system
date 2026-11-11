package com.ga.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    // from UserProfile (null when the user has no profile yet)
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImageUrl;
}
