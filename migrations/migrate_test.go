package migrations

import (
	"context"
	"database/sql"
	"io/fs"
	"path/filepath"
	"testing"
	"testing/fstest"

	_ "modernc.org/sqlite"
)

func openTestDB(t *testing.T) *sql.DB {
	t.Helper()
	db, err := sql.Open("sqlite", filepath.Join(t.TempDir(), "test.sqlite"))
	if err != nil {
		t.Fatal(err)
	}
	db.SetMaxOpenConns(1)
	t.Cleanup(func() { db.Close() })
	return db
}

func versionCount(t *testing.T, db *sql.DB, version int) int {
	t.Helper()
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM goose_db_version WHERE version_id = ? AND is_applied = 1`, version).Scan(&count); err != nil {
		t.Fatal(err)
	}
	return count
}

func TestMigrationsApplyOnceAndPreserveData(t *testing.T) {
	ctx := context.Background()
	db := openTestDB(t)
	if err := Up(ctx, db); err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`INSERT INTO short_urls (original_url, short_code, is_active) VALUES ('https://ctailee.com/', 'test001', 1)`); err != nil {
		t.Fatal(err)
	}
	if err := Up(ctx, db); err != nil {
		t.Fatal(err)
	}
	if count := versionCount(t, db, 1); count != 1 {
		t.Fatalf("version 1 applied %d times", count)
	}
	var original string
	if err := db.QueryRow(`SELECT original_url FROM short_urls WHERE short_code = 'test001'`).Scan(&original); err != nil {
		t.Fatal(err)
	}
	if original != "https://ctailee.com/" {
		t.Fatal(original)
	}
}

func TestNewMigrationAndRollbackOnFailure(t *testing.T) {
	ctx := context.Background()
	db := openTestDB(t)
	if err := Up(ctx, db); err != nil {
		t.Fatal(err)
	}
	first, err := fs.ReadFile(files, "00001_create_short_urls.sql")
	if err != nil {
		t.Fatal(err)
	}
	pending := fstest.MapFS{
		"00001_create_short_urls.sql": &fstest.MapFile{Data: first},
		"00002_add_description.sql":   &fstest.MapFile{Data: []byte("-- +goose Up\nALTER TABLE short_urls ADD COLUMN description TEXT;\n-- +goose Down\nALTER TABLE short_urls DROP COLUMN description;\n")},
	}
	if err := up(ctx, db, pending); err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`INSERT INTO short_urls (original_url, short_code, description) VALUES ('https://ctailee.com/', 'test002', 'new column')`); err != nil {
		t.Fatal(err)
	}
	if versionCount(t, db, 1) != 1 || versionCount(t, db, 2) != 1 {
		t.Fatal("unexpected migration history")
	}
	pending["00003_invalid.sql"] = &fstest.MapFile{Data: []byte("-- +goose Up\nCREATE TABLE should_rollback (id INTEGER);\nINSERT INTO missing_table VALUES (1);\n-- +goose Down\nDROP TABLE should_rollback;\n")}
	if err := up(ctx, db, pending); err == nil {
		t.Fatal("expected migration failure")
	}
	if versionCount(t, db, 3) != 0 {
		t.Fatal("failed migration recorded as applied")
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM sqlite_master WHERE name = 'should_rollback'`).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 0 {
		t.Fatal("failed migration was not rolled back")
	}
}
