package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
            CategoryFiltersDTO filters,
            Pageable pageable
    ) {
        PageResponseDTO<ResponseCategoryDTO> response = categoryService.list(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponseDTO<ResponseCategoryDTO>> listDeleted(
            CategoryFiltersDTO filters,
            Pageable pageable
    ) {
        PageResponseDTO<ResponseCategoryDTO> response = categoryService.listDeleted(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseCategoryDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryDTO data
    ) {
        ResponseCategoryDTO response = categoryService.update(id, data);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        categoryService.restore(id);
        return ResponseEntity.noContent().build();
    }
}
