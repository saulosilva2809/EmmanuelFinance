package com.emmanuelfinance.category;

import com.emmanuelfinance.category.enums.TypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    List<Category> findAllByUserId(UUID userId);
    boolean existsByNameIgnoreCaseAndTypeAndUserId(String name, TypeEnum type, UUID userId);
}
