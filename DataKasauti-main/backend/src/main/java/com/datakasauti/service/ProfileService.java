package com.datakasauti.service;

import com.datakasauti.dto.response.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    ProfileResponse profile(MultipartFile file);
}
