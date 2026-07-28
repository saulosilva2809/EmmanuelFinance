ALTER TABLE category
ADD CONSTRAINT uk_category_name_type UNIQUE (name, type);