package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantCategoriesVO {
    @NotBlank(message = "Merchant category code is required")
    @Size(min = 4, max = 4, message = "MCC length should be 4")
    private String merchantCategoryCode;

    private String lastUpdatedDate;

    private Integer updatedUser;

    @NotBlank(message = "Description is required")
    @Size(max = 150, message = "Description should be within 150 characters")
    private String merchantCategoryDescription;

    private String merchantCategoryTcc;

    @Size(max = 50, message = "Business segment length should be within 50 characters")
    private String businessSegments;

    private Integer priority;

    private String merchantCategoryStatus;
}
