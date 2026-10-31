package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.RowChangesDto;
import com.datakasauti.dto.response.WarningDto;
import java.util.List;

public record KeyComparison(RowChangesDto rowChanges, List<WarningDto> warnings) {
}
