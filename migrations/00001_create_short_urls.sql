-- +goose Up
CREATE TABLE IF NOT EXISTS short_urls (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    original_url TEXT NOT NULL,
    short_code TEXT NOT NULL,
    created_at DATETIME,
    expires_at DATETIME,
    is_active NUMERIC
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_short_urls_short_code ON short_urls (short_code);

-- +goose Down
DROP TABLE short_urls;
