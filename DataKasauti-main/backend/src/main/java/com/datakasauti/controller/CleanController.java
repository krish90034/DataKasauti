 package com.datakasauti.controller;

import com.datakasauti.dto.request.CleanRequest;
import com.datakasauti.dto.response.CleanPreviewResponse;
import com.datakasauti.exception.CsvApiException;
import com.datakasauti.service.CleanExport;
import com.datakasauti.service.CleanService;
import com.datakasauti.validation.CsvFileValidator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CleanController {

    private static final Logger log = LoggerFactory.getLogger(CleanController.class);

    private final CleanService cleanService;
    private final ObjectMapper objectMapper;
    private final CsvFileValidator fileValidator;

    public CleanController(
            CleanService cleanService,
            ObjectMapper objectMapper,
            CsvFileValidator fileValidator
    ) {
        this.cleanService = cleanService;
        this.objectMapper = objectMapper;
        this.fileValidator = fileValidator;
    }

    @PostMapping(
            value = "/clean/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CleanPreviewResponse preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam("rules") String rulesJson
    ) {
        fileValidator.validate(file);
        CleanRequest request = parseRules(rulesJson);
        return cleanService.preview(file, request);
    }

    @PostMapping(
            value = "/clean/export",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam("file") MultipartFile file,
            @RequestParam("rules") String rulesJson
    ) {
        fileValidator.validate(file);
        CleanRequest request = parseRules(rulesJson);
        CleanExport export = cleanService.export(file, request);

        StreamingResponseBody body = outputStream -> {
            try (InputStream input = Files.newInputStream(export.tempFile())) {
                input.transferTo(outputStream);
                outputStream.flush();
            } finally {
                Files.deleteIfExists(export.tempFile());
            }
        };

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.downloadName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    private CleanRequest parseRules(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            throw invalidRules("The rules field is required and must contain JSON.");
        }

        try {
            CleanRequest request = objectMapper.readValue(rulesJson, CleanRequest.class);

            if (request == null || request.rules() == null || request.rules().isEmpty()) {
                throw invalidRules("Provide at least one cleaning rule.");
            }

            return request;
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Could not parse cleaning rules JSON ({} characters): {}",
                    rulesJson.length(),
                    exception.getOriginalMessage()
            );
            throw invalidRules("The rules field contains invalid JSON.");
        }
    }

    private CsvApiException invalidRules(String message) {
        return new CsvApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_RULE",
                message,
                Map.of()
        );
    }
}
