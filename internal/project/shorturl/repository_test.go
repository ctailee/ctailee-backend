package shorturl

import (
	"context"
	"database/sql"
	"errors"
	"github.com/ctailee/ctailee-backend/internal/database"
	"github.com/ctailee/ctailee-backend/migrations"
	"path/filepath"
	"testing"
	"time"
)

func TestSQLitePersistence(t *testing.T) {
	ctx := context.Background()
	path := filepath.Join(t.TempDir(), "shorturl.sqlite")
	service, db, err := openTestService(t, ctx, path)
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	code, err := service.Create(ctx, "https://ctailee.com/")
	if err != nil {
		t.Fatal(err)
	}
	repo := service.repository.(*sqliteRepository)
	entry, err := repo.find(ctx, code)
	if err != nil {
		t.Fatal(err)
	}
	if entry.ID == 0 || entry.CreatedAt.IsZero() || entry.ExpiresAt == nil {
		t.Fatalf("missing stored fields: %+v", entry)
	}
	if err := repo.insert(ctx, entry); !errors.Is(err, errDuplicateCode) {
		t.Fatalf("duplicate = %v", err)
	}
	if err := db.Close(); err != nil {
		t.Fatal(err)
	}
	service, db, err = openTestService(t, ctx, path)
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	if got, err := service.GetOriginalURL(ctx, code); err != nil || got != "https://ctailee.com/" {
		t.Fatalf("reopened = %q, %v", got, err)
	}
	repo = service.repository.(*sqliteRepository)
	if err := repo.insert(ctx, shortURL{OriginalURL: "https://ctailee.com/permanent", ShortCode: "noexp01", IsActive: true}); err != nil {
		t.Fatal(err)
	}
	if got, err := service.GetOriginalURL(ctx, "noexp01"); err != nil || got != "https://ctailee.com/permanent" {
		t.Fatalf("no expiration = %q, %v", got, err)
	}
	past := time.Now().Add(-time.Hour)
	if err := repo.insert(ctx, shortURL{OriginalURL: "https://ctailee.com/expired", ShortCode: "expired", ExpiresAt: &past, IsActive: true}); err != nil {
		t.Fatal(err)
	}
	if _, err := service.GetOriginalURL(ctx, "expired"); !errors.Is(err, ErrNotFound) {
		t.Fatal(err)
	}
	var active bool
	if err := db.QueryRowContext(ctx, `SELECT is_active FROM short_urls WHERE short_code='expired'`).Scan(&active); err != nil {
		t.Fatal(err)
	}
	if active {
		t.Fatal("expiration was not persisted")
	}
	if _, err := service.GetOriginalURL(ctx, "missing"); !errors.Is(err, ErrNotFound) {
		t.Fatal(err)
	}
}

func openTestService(t *testing.T, ctx context.Context, path string) (*Service, *sql.DB, error) {
	t.Helper()
	orm, db, err := database.OpenSQLite(ctx, path)
	if err != nil {
		return nil, nil, err
	}
	if err := migrations.Up(ctx, db); err != nil {
		db.Close()
		return nil, nil, err
	}
	return New(orm), db, nil
}
