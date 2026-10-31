package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.SchemaChangeDto;
import com.datakasauti.dto.response.WarningDto;
import java.util.List;

public record SchemaComparison(
        List<String> addedColumns,
        List<String> removedColumns,
        List<SchemaChangeDto> typeChanges,
        List<WarningDto> warnings
) {
}
