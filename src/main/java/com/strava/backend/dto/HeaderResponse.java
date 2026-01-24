package com.strava.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

import java.util.List;

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
