package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.CommonColumnDto;
import com.datakasauti.dto.response.CompareResponse;
import com.datakasauti.dto.response.FileSummaryDto;
import com.datakasauti.dto.response.RowChangesDto;
import com.datakasauti.dto.response.SchemaChangeDto;
import com.datakasauti.dto.response.WarningDto;
import java.util.List;

/** Engine result for a two-file comparison; the service converts it to the API response. */
public record CompareResult(
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
    public CompareResult {
        addedColumns = List.copyOf(addedColumns);
        removedColumns = List.copyOf(removedColumns);
        typeChanges = List.copyOf(typeChanges);
        commonColumns = List.copyOf(commonColumns);
        warnings = List.copyOf(warnings);
    }

    public CompareResponse toResponse() {
        return new CompareResponse(baseline, current, rowCountDelta, addedColumns,
                removedColumns, typeChanges, commonColumns, rowChanges, warnings);
    }
}
