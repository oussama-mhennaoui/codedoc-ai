package com.codedoc.user.dto;

public record AuthResponse(
        String token,
        UserDto user
) {
}
