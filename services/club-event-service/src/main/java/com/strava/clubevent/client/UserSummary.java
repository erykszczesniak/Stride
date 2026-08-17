package com.strava.clubevent.client;

import com.strava.clubevent.common.BaseUserResponse;

/**
 * Shape of one element returned by user-service's internal batch lookup
 * ({@code UserInternalService.GetUserSummaries}); absent proto3 optional
 * fields arrive here as nulls, exactly like the old JSON contract.
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
