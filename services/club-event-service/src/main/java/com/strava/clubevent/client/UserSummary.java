package com.strava.clubevent.client;

import com.strava.clubevent.common.BaseUserResponse;

/**
 * Shape of one element returned by user-service's internal batch endpoint
 * {@code GET /internal/users?ids=1,2,3}.
 */
public record UserSummary(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String avatar
) {

    /**
     * Maps to the public BaseUserResponse contract. {@code unknown(id)} covers
     * ids the batch lookup could not resolve (user-service down or user
     * deleted) — rendered "User #id" so member lists stay usable.
     */
    public BaseUserResponse toBaseUserResponse() {
        BaseUserResponse response = new BaseUserResponse();
        response.setId(id);
        response.setFirstName(firstName);
        response.setLastName(lastName);
        response.setAvatar(avatar);
        return response;
    }

    public static BaseUserResponse unknown(Long id) {
        BaseUserResponse response = new BaseUserResponse();
        response.setId(id);
        response.setFirstName("User");
        response.setLastName("#" + id);
        return response;
    }
}
