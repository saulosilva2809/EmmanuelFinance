ALTER TABLE category DROP CONSTRAINT uk_category_name_type;

ALTER TABLE category ADD CONSTRAINT uk_category_name_type_user
    UNIQUE (name, type, user_id);