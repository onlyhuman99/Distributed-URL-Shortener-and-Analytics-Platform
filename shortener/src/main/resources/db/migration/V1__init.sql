CREATE TABLE app_user (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE short_url (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(16) UNIQUE,
  original_url VARCHAR(2048) NOT NULL,
  owner_id BIGINT NOT NULL REFERENCES app_user(id),
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE click_event (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(16) NOT NULL,
  clicked_at TIMESTAMPTZ NOT NULL,
  user_agent VARCHAR(512),
  referrer VARCHAR(2048)
);
CREATE INDEX idx_click_event_code_time ON click_event(code, clicked_at);