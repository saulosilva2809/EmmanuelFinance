package com.emmanuelfinance.category.controllers;

import com.emmanuelfinance.category.services.CategoryInternalService;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class CategoryInternalController {

    private final CategoryInternalService categoryInternalService;

    @GetMapping("/summary/{id}")
    public ResponseEntity<CategorySummaryDTO> getCategorySummary(@PathVariable UUID id) {
        CategorySummaryDTO response = categoryInternalService.getCategorySummary(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal-summary/{id}")
    public ResponseEntity<CategoryInternalSummaryDTO> getCategoryInternalSummary(@PathVariable UUID id) {
        CategoryInternalSummaryDTO response = categoryInternalService.getCategoryInternalSummary(id);
        return ResponseEntity.ok(response);
    }
}
