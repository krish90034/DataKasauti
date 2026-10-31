 
package com.datakasauti;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SuppressWarnings("null")
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String SAMPLE_CSV = """
            id,sales,city
            1,120.50,Delhi
            2,200.00,Lucknow
            3,,Delhi
            3,,Delhi
            """;

    private static final String BASELINE_CSV = """
            id,sales,city
            1,100,Delhi
            2,200,Lucknow
            3,300,Delhi
            """;

    private static final String CURRENT_CSV = """
            id,sales,region
            1,110,Delhi-North
            2,200,Lucknow
            4,400,Mumbai
            """;

    private static final String CLEAN_RULES = """
            {"rules":[
                {"type":"TRIM_WHITESPACE"},
                {"type":"REMOVE_DUPLICATES"},
                {"type":"FILL_MISSING","column":"sales","strategy":"MEAN"}
            ]}
            """;

    private MockMultipartFile csv(
            String partName,
            String filename,
            String content
    ) {
        return new MockMultipartFile(
                partName,
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.max_upload_mb").value(10));
    }

    @Test
    void profileEndpointCalculatesCsvStatistics() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/profile")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("sample.csv"))
                .andExpect(jsonPath("$.row_count").value(4))
                .andExpect(jsonPath("$.column_count").value(3))
                .andExpect(jsonPath("$.duplicate_rows").value(1))
                .andExpect(jsonPath("$.total_missing_values").value(2))
                .andExpect(jsonPath("$.missing_values.sales").value(2))
                .andExpect(jsonPath("$.columns[0].detected_type")
                        .value("INTEGER"))
                .andExpect(jsonPath("$.preview_rows.length()").value(4));
    }

    @Test
    void compareEndpointDetectsSchemaAndRowChanges() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/compare")
                                .file(csv(
                                        "baselineFile",
                                        "baseline.csv",
                                        BASELINE_CSV
                                ))
                                .file(csv(
                                        "currentFile",
                                        "current.csv",
                                        CURRENT_CSV
                                ))
                                .param("key_column", "id")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline.row_count").value(3))
                .andExpect(jsonPath("$.current.row_count").value(3))
                .andExpect(jsonPath("$.row_count_delta").value(0))
                .andExpect(jsonPath("$.added_columns[0]").value("region"))
                .andExpect(jsonPath("$.removed_columns[0]").value("city"))
                .andExpect(jsonPath("$.row_changes.added_keys_count").value(1))
                .andExpect(jsonPath("$.row_changes.removed_keys_count").value(1))
                .andExpect(jsonPath("$.row_changes.unchanged_keys_count").value(1))
                .andExpect(jsonPath("$.row_changes.changed_keys_count").value(1));
    }

    @Test
    void cleanPreviewRemovesDuplicatesAndFillsMissingValues()
            throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/clean/preview")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                                .param("rules", CLEAN_RULES)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows_before").value(4))
                .andExpect(jsonPath("$.rows_after").value(3))
                .andExpect(jsonPath("$.original_modified").value(false))
                .andExpect(jsonPath("$.rule_results[1].rows_affected").value(1))
                .andExpect(jsonPath("$.rule_results[2].cells_changed").value(1))
                .andExpect(jsonPath("$.sample_changes[0].after").value("160.25"));
    }

    @Test
    void cleanExportReturnsCleanedCsv() throws Exception {
        MvcResult pendingResult = mockMvc.perform(
                        multipart("/api/v1/clean/export")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                                .param("rules", CLEAN_RULES)
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pendingResult))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("sample_cleaned.csv")
                ))
                .andExpect(content().string(
                        "id,sales,city\n"
                                + "1,120.50,Delhi\n"
                                + "2,200.00,Lucknow\n"
                                + "3,160.25,Delhi\n"
                ));
    }

    @Test
    void unknownCleaningRuleReturnsBadRequest() throws Exception {
        String invalidRules =
                "{\"rules\":[{\"type\":\"UNKNOWN_RULE\"}]}";

        mockMvc.perform(
                        multipart("/api/v1/clean/preview")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                                .param("rules", invalidRules)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_RULE"))
                .andExpect(jsonPath("$.message")
                        .value(containsString("Unknown cleaning rule type")));
    }

    @Test
    void missingProfileFileReturnsContractErrorShape() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/profile")
                                .param("unused", "value")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("FILE_MISSING"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0]").value("part: file"));
    }

    @Test
    void emptyProfileFileReturnsFileEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/profile").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("FILE_EMPTY"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void unsupportedExtensionReturnsUnsupportedFileType() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/profile")
                                .file(csv("file", "not-a-csv.txt", SAMPLE_CSV))
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error_code").value("UNSUPPORTED_FILE_TYPE"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void duplicateHeadersReturnDuplicateHeaders() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/profile")
                                .file(csv("file", "duplicate.csv", "id,id\n1,2\n"))
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code").value("DUPLICATE_HEADERS"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").value(hasItem(containsString("duplicate_headers"))));
    }

    @Test
    void rowsWithTooManyFieldsReturnCsvParseError() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/profile")
                                .file(csv("file", "long-row.csv", "id,name\n1,Alice,extra\n"))
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code").value("CSV_PARSE_ERROR"));
    }

    @Test
    void moreThanFivePercentShortRowsReturnCsvParseError() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/profile")
                                .file(csv("file", "short-rows.csv", "id,name\n1,Alice\n2\n"))
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code").value("CSV_PARSE_ERROR"));
    }

    @Test
    void missingRulesParameterReturnsInvalidRule() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/clean/preview")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_RULE"))
                .andExpect(jsonPath("$.details[0]").value("parameter: rules"));
    }

    @Test
    void malformedRulesJsonReturnsInvalidRule() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/clean/preview")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                                .param("rules", "not-json")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_RULE"));
    }

    @Test
    void emptyRulesArrayReturnsInvalidRule() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/clean/preview")
                                .file(csv("file", "sample.csv", SAMPLE_CSV))
                                .param("rules", "{\"rules\":[]}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_RULE"));
    }

    @Test
    void exportProtectsFormulaCellsButPreservesNegativeNumbers() throws Exception {
        String formulaCsv = "id,note\n1,\"=SUM(1,2)\"\n2,-5\n";
        String rules = "{\"rules\":[{\"type\":\"REMOVE_DUPLICATES\"}]}";

        MvcResult pendingResult = mockMvc.perform(
                        multipart("/api/v1/clean/export")
                                .file(csv("file", "formula.csv", formulaCsv))
                                .param("rules", rules)
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pendingResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("'=SUM(1,2)")))
                .andExpect(content().string(containsString("2,-5")));
    }


    @Test
    void fileLargerThanTenMegabytesReturnsFileTooLarge() throws Exception {
        byte[] oversizedContent = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "oversized.csv",
                "text/csv",
                oversizedContent
        );

        mockMvc.perform(multipart("/api/v1/profile").file(oversizedFile))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.error_code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void unsupportedFileContentTypeReturnsUnsupportedFileType() throws Exception {
        MockMultipartFile pdfMimeFile = new MockMultipartFile(
                "file",
                "data.csv",
                "application/pdf",
                SAMPLE_CSV.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/profile").file(pdfMimeFile))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error_code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void missingComparePartReturnsFileMissingWithPartName() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/compare")
                                .file(csv("baselineFile", "baseline.csv", BASELINE_CSV))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("FILE_MISSING"))
                .andExpect(jsonPath("$.details").value(hasItem(containsString("currentFile"))));
    }

}
