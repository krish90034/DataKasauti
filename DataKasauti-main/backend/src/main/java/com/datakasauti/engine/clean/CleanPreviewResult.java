package com.datakasauti.engine.clean;

import com.datakasauti.dto.response.CleanPreviewResponse;
import java.util.ArrayList;
import java.util.List;

/** Cleaned in-memory dataset and the preview calculated from those exact rows. */
public record CleanPreviewResult(
        List<String> headers,
        List<List<String>> rows,
        CleanPreviewResponse preview
) {
    public CleanPreviewResult {
        headers = List.copyOf(headers);
        List<List<String>> copy = new ArrayList<>(rows.size());
        for (List<String> row : rows) copy.add(List.copyOf(row));
        rows = List.copyOf(copy);
    }
}
