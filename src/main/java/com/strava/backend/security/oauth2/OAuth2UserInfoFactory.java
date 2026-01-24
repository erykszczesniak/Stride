package com.strava.backend.security.oauth2;

import com.strava.backend.exception.ApiRequestException;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new ApiRequestException("Login with " + registrationId + " is not supported", HttpStatus.BAD_REQUEST);
        }
    }
}
