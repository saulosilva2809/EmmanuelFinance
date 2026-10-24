package com.emmanuelfinance.category.query;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.mongo.BaseDocument;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "categories")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class CategoryDocument extends BaseDocument {

    private UUID userId;
    private String name;
    private TypeEnum type;
}
