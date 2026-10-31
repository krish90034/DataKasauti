 package com.datakasauti.controller;

import com.datakasauti.dto.response.CompareResponse;
import com.datakasauti.service.CompareService;
import com.datakasauti.validation.CsvFileValidator;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CompareController {

    private final CompareService compareService;
    private final CsvFileValidator fileValidator;

    public CompareController(
            CompareService compareService,
            CsvFileValidator fileValidator
    ) {
        this.compareService = compareService;
        this.fileValidator = fileValidator;
    }

    @PostMapping(
            value = "/compare",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CompareResponse compare(
            @RequestParam("baselineFile") MultipartFile baselineFile,
            @RequestParam("currentFile") MultipartFile currentFile,
            @RequestParam(value = "key_column", required = false) String keyColumn
    ) {
        fileValidator.validate(baselineFile);
        fileValidator.validate(currentFile);

        return compareService.compare(
                baselineFile,
                currentFile,
                keyColumn
        );
    }
}
