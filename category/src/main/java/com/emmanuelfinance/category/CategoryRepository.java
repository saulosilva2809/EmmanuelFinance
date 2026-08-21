package com.emmanuelfinance.category;

import com.emmanuelfinance.shared.enums.TypeEnum;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    @Query(value = "SELECT * FROM category WHERE id = :id AND user_id = :userId", nativeQuery = true)
    Optional<Category> findByIdAndUserIdIncludingDeleted(@Param("id") UUID id, @org.springframework.data.repository.query.Param("userId") UUID userId);

    boolean existsByNameIgnoreCaseAndTypeAndUserId(String name, TypeEnum type, UUID userId);
}
