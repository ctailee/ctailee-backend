package database

import (
	"context"
	"path/filepath"
	"testing"
)

func TestOpenSQLiteSharedConnection(t *testing.T) {
	orm, db, err := OpenSQLite(context.Background(), filepath.Join(t.TempDir(), "app.sqlite"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { db.Close() })
	pool, err := orm.DB()
	if err != nil || pool != db {
		t.Fatalf("ORM does not share the SQL pool: %v", err)
	}
	var timeout, tables int
	if err := db.QueryRow("PRAGMA busy_timeout").Scan(&timeout); err != nil || timeout != 5000 {
		t.Fatalf("busy_timeout = %d, %v", timeout, err)
	}
	if err := db.QueryRow("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'").Scan(&tables); err != nil || tables != 0 {
		t.Fatalf("unexpected application schema: %d tables, %v", tables, err)
	}
	if err := orm.Exec("CREATE TABLE other_service (id INTEGER PRIMARY KEY)").Error; err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec("INSERT INTO other_service (id) VALUES (1)"); err != nil {
		t.Fatal(err)
	}
	var count int64
	if err := orm.Table("other_service").Count(&count).Error; err != nil || count != 1 {
		t.Fatalf("shared data count = %d, %v", count, err)
	}
}

func TestOpenSQLiteFailure(t *testing.T) {
	orm, db, err := OpenSQLite(context.Background(), filepath.Join(t.TempDir(), "missing", "app.sqlite"))
	if err == nil {
		db.Close()
		t.Fatal("expected open failure")
	}
	if orm != nil || db != nil {
		t.Fatal("failed open returned a database")
	}
}
