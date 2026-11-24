package com.empay.user_management.valueobjects;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String type;
    private String message;
    private String path;
    private List<Map<String, String>> errors;
}

