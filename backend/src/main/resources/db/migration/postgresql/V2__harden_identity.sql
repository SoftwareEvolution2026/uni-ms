ALTER TABLE users RENAME COLUMN full_name TO name;
ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users RENAME COLUMN roles TO role;

UPDATE users
SET role = CASE role
    WHEN 'ROLE_ADMIN' THEN 'ADMIN'
    WHEN 'ROLE_ACADEMIC_MANAGER' THEN 'ACADEMIC_MANAGER'
    ELSE role
END;

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'ACADEMIC_MANAGER')),
    ADD CONSTRAINT chk_users_email_normalized CHECK (email = lower(btrim(email)));

ALTER TABLE refresh_tokens
    ADD COLUMN family_id UUID,
    ADD COLUMN replaced_by_token_hash VARCHAR(64),
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE refresh_tokens SET family_id = gen_random_uuid() WHERE family_id IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_active_user
    ON refresh_tokens (user_id, revoked, expires_at);
