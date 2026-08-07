package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFIltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ResponseCategoryDTO> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCategoryDTO data
    ) {
        ResponseCategoryDTO response = categoryService.create(jwt, data);
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

    @PutMapping("/{id}")
    public ResponseEntity<ResponseCategoryDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryDTO data
    ) {
        ResponseCategoryDTO response = categoryService.update(id, data);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
