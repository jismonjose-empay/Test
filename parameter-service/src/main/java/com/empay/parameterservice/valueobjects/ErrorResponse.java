package com.empay.parameterservice.valueobjects;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

