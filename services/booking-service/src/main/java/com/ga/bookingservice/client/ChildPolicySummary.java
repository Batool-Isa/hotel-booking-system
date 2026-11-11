package com.ga.bookingservice.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChildPolicySummary {
    private Integer infantMaxAge;
    private Integer childMaxAge;
    private boolean infantsCountTowardOccupancy;
}
