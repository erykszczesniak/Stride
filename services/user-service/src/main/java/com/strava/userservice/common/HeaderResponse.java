package com.strava.userservice.common;

import org.springframework.http.HttpHeaders;

import java.util.List;

/**
 * Paged-list envelope mirrored from the monolith's {@code HeaderResponse}.
 *
 * <p>Used two ways, exactly like the monolith:
 * <ul>
 *   <li>public list endpoints return {@link #items()} as the body and {@link #headers()}
 *       as HTTP response headers ({@code page-total-count} / {@code page-total-elements});</li>
 *   <li>admin list endpoints return the whole envelope as the JSON body — the frontend
 *       reads {@code response.data.items}.</li>
 * </ul>
 */
public record HeaderResponse<T>(List<T> items, HttpHeaders headers) {

    public static <T> HeaderResponse<T> of(List<T> items, int totalPages, long totalElements) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("page-total-count", String.valueOf(totalPages));
        headers.add("page-total-elements", String.valueOf(totalElements));
        return new HeaderResponse<>(items, headers);
    }
}
