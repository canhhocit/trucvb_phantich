CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    agency_id BIGINT NOT NULL REFERENCES organizations(id),
    key_id VARCHAR(64) NOT NULL UNIQUE,
    secret_enc TEXT NOT NULL,
    secret_hint VARCHAR(8) NOT NULL,
    algorithm VARCHAR(32) NOT NULL DEFAULT 'HMAC_SHA256',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_agency ON api_keys(agency_id, status);
