ALTER TABLE users
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('MEMBER', 'ADMIN'));

CREATE INDEX idx_users_role
    ON users (role);
