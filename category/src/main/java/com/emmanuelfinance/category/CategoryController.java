package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFIltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ResponseCategoryDTO> create(@Valid @RequestBody CreateCategoryDTO data) {
        ResponseCategoryDTO response = categoryService.create(data);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseCategoryDTO> view(@PathVariable UUID id) {
        ResponseCategoryDTO response = categoryService.view(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ResponseCategoryDTO>> list(
            CategoryFIltersDTO filters,
            Pageable pageable
    ) {
        PageResponseDTO<ResponseCategoryDTO> response = categoryService.list(filters, pageable);
        return ResponseEntity.ok().body(response);
    }
}
