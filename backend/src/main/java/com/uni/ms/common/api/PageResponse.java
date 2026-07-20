package com.uni.ms.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<SortResponse> sort
) {
    public static <T> PageResponse<T> from(Page<?> source, List<T> content) {
        List<SortResponse> sort = source.getSort().stream()
                .map(order -> new SortResponse(order.getProperty(), order.getDirection().name()))
                .toList();
        return new PageResponse<>(content, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages(), source.isFirst(),
                source.isLast(), sort);
    }
}
