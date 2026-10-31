package com.datakasauti.service;

import com.datakasauti.dto.response.CompareResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CompareService {

    CompareResponse compare(
            MultipartFile baselineFile,
            MultipartFile currentFile,
            String keyColumn
    );
}