package com.datakasauti.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "datakasauti.upload")
public class UploadProperties {

    private String tempDir;
    private String allowedExtensions = "csv";
    private int maxUploadMb = 10;
    private int previewRows = 20;
    private int maxRows = 200000;
    private int maxColumns = 200;
    private int staleFileMinutes = 60;

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public String getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(String allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public int getMaxUploadMb() {
        return maxUploadMb;
    }

    public void setMaxUploadMb(int maxUploadMb) {
        this.maxUploadMb = maxUploadMb;
    }

    public int getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(int previewRows) {
        this.previewRows = previewRows;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getMaxColumns() {
        return maxColumns;
    }

    public void setMaxColumns(int maxColumns) {
        this.maxColumns = maxColumns;
    }

    public int getStaleFileMinutes() {
        return staleFileMinutes;
    }

    public void setStaleFileMinutes(int staleFileMinutes) {
        this.staleFileMinutes = staleFileMinutes;
    }
}