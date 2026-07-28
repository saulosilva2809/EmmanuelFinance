package com.emmanuelfinance.category;

import com.emmanuelfinance.category.enums.TypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    boolean existsByNameIgnoreCaseAndType(String name, TypeEnum type);
}
