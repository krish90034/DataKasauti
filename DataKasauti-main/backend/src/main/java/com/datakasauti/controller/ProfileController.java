 package com.datakasauti.controller;

import com.datakasauti.dto.response.ProfileResponse;
import com.datakasauti.service.ProfileService;
import com.datakasauti.validation.CsvFileValidator;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService profileService;
    private final CsvFileValidator fileValidator;

    public ProfileController(
            ProfileService profileService,
            CsvFileValidator fileValidator
    ) {
        this.profileService = profileService;
        this.fileValidator = fileValidator;
    }

    @PostMapping(
            value = "/profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ProfileResponse profile(
            @RequestParam("file") MultipartFile file
    ) {
        fileValidator.validate(file);
        return profileService.profile(file);
    }
}
