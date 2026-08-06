CREATE TABLE category (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    account_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);