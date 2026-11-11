package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PageableFactory {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("updatedAt", "title");

    public Pageable create(Integer requestedPage, Integer requestedSize, Sort sort) {
        int page = requestedPage == null ? DEFAULT_PAGE : requestedPage;
        int size = requestedSize == null ? DEFAULT_SIZE : requestedSize;
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page không hợp lệ");
        }
        if (size < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "size không hợp lệ");
        }

        Sort safeSort = sort == null ? Sort.unsorted() : sort;
        if (safeSort.stream().anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trường sắp xếp không hợp lệ");
        }
        return PageRequest.of(page, Math.min(size, MAX_SIZE), safeSort);
    }
}
