package com.paypulse.platform.util;

public record PaginationWindow(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalRecords,
        int fromIndex,
        int toIndex,
        boolean hasNextPage,
        boolean hasPreviousPage
) {
}