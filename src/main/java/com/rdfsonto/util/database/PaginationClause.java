package com.rdfsonto.util.database;

import lombok.Builder;
import lombok.RequiredArgsConstructor;


@Builder(setterPrefix = "with", toBuilder = true)
@RequiredArgsConstructor
public class PaginationClause
{
    final int pageSize;
    final int page;

    public String createPaginationClause()
    {
        final var skip = page * pageSize;
        return "SKIP %s LIMIT %s".formatted(skip, pageSize);
    }
}
