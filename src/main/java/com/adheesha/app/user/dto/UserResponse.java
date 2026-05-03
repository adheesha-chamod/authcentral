package com.adheesha.app.user.dto;

public record UserResponse(
    String userId,
    String username,
    String firstName,
    String lastName,
    String address,
    String usertype
) {}
