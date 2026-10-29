package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.User;

public record UserProfileResponse(Long userId, String name, String email, String role,
                                  Integer cleanCoins, Integer totalReports) {

    public static UserProfileResponse from(User u) {
        return new UserProfileResponse(u.getUserId(), u.getName(), u.getEmail(), u.getRole().name(),
                u.getCleanCoins() == null ? 0 : u.getCleanCoins(),
                u.getTotalReports() == null ? 0 : u.getTotalReports());
    }
}
