package com.datakasauti.service;

import com.datakasauti.dto.request.CleanRequest;
import com.datakasauti.dto.response.CleanPreviewResponse;

import org.springframework.web.multipart.MultipartFile;

public interface CleanService {

    CleanPreviewResponse preview(
            MultipartFile file,
            CleanRequest request
    );

    CleanExport export(
            MultipartFile file,
            CleanRequest request
    );
}