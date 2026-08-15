package com.strava.activityservice.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

import java.util.List;

/**
 * Ported 1:1 from main. List endpoints send it two ways, both mirrored here:
 *   * as headers + plain JSON array body (activities lists) — the controller
 *     unwraps items/headers;
 *   * serialized whole as the body (admin lists) — the frontend reads
 *     data.items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeaderResponse<T> {

    private List<T> items;
    private HttpHeaders headers;

    public static <T> HeaderResponse<T> of(List<T> items, int totalPages, long totalElements) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("page-total-count", String.valueOf(totalPages));
        headers.add("page-total-elements", String.valueOf(totalElements));
        return new HeaderResponse<>(items, headers);
    }
}
