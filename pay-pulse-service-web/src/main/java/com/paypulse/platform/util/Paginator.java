package com.paypulse.platform.util;

import org.springframework.stereotype.Component;

@Component
public class Paginator {

    public PaginationWindow paginate(int totalRecords, Integer requestedPage, Integer requestedPageSize) {
        int safePageSize = requestedPageSize == null || requestedPageSize < 1 ? 20 : requestedPageSize;
        int safePage = requestedPage == null || requestedPage < 1 ? 1 : requestedPage;

        int totalPages = totalRecords == 0 ? 0 : (int) Math.ceil((double) totalRecords / safePageSize);

        if (totalPages == 0) {
            return new PaginationWindow(
                    1,
                    safePageSize,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false
            );
        }

        int boundedPage = Math.min(safePage, totalPages);
        int fromIndex = (boundedPage - 1) * safePageSize;
        int toIndex = Math.min(fromIndex + safePageSize, totalRecords);

        return new PaginationWindow(
                boundedPage,
                safePageSize,
                totalPages,
                totalRecords,
                fromIndex,
                toIndex,
                boundedPage < totalPages,
                boundedPage > 1
        );
    }
}