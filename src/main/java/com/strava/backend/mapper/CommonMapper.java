package com.strava.backend.mapper;

import com.strava.backend.dto.HeaderResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommonMapper {

    private final ModelMapper modelMapper;

    public <T, S> S convertToEntity(T data, Class<S> type) {
        return modelMapper.map(data, type);
    }

    public <T, S> S convertToResponse(T data, Class<S> type) {
        return modelMapper.map(data, type);
    }

    public <T, S> List<S> convertToResponseList(List<T> items, Class<S> type) {
        return items.stream()
                .map(item -> convertToResponse(item, type))
                .collect(Collectors.toList());
    }

    public <T, S> HeaderResponse<S> getHeaderResponse(Page<T> page, Class<S> type) {
        List<S> items = page.getContent().stream()
                .map(item -> convertToResponse(item, type))
                .collect(Collectors.toList());
        return HeaderResponse.of(items, page.getTotalPages(), page.getTotalElements());
    }

    public <T, S> HeaderResponse<S> getHeaderResponse(List<T> items, int totalPages, long totalElements, Class<S> type) {
        List<S> responseItems = items.stream()
                .map(item -> convertToResponse(item, type))
                .collect(Collectors.toList());
        return HeaderResponse.of(responseItems, totalPages, totalElements);
    }
}
