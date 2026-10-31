package com.datakasauti.dto.response;

import java.util.List;

public record CompareResponse(
        FileSummaryDto baseline,
        FileSummaryDto current,
        long rowCountDelta,
        List<String> addedColumns,
        List<String> removedColumns,
        List<SchemaChangeDto> typeChanges,
        List<CommonColumnDto> commonColumns,
        RowChangesDto rowChanges,
        List<WarningDto> warnings
) {
}