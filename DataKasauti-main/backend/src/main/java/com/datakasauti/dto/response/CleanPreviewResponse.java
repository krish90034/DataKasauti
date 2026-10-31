package com.datakasauti.dto.response;

import java.util.List;

public record CleanPreviewResponse(
        List<String> columns,
        long rowsBefore,
        long rowsAfter,
        List<RuleResultDto> ruleResults,
        List<SampleChangeDto> sampleChanges,
        List<RemovedRowDto> removedRowsSample,
        boolean originalModified
) {
}