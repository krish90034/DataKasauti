package com.datakasauti.dto.response;

public record RowChangesDto(
        String keyColumn,
        long addedKeysCount,
        long removedKeysCount,
        long unchangedKeysCount,
        long changedKeysCount
) {
}