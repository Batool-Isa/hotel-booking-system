package com.ga.billingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSummary {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImageUrl;

    public String getFullName() {
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        String name = (first + " " + last).trim();
        return name.isEmpty() ? username : name;
    }
}
