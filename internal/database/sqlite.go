package database

import (
	"context"
	"database/sql"

	gormsqlite "gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
	_ "modernc.org/sqlite"
)

// OpenSQLite opens a shared SQLite database without applying application migrations.
// The caller owns and must close the returned SQL connection pool.
func OpenSQLite(ctx context.Context, path string) (*gorm.DB, *sql.DB, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return nil, nil, err
	}
	// Serialize writes and keep connection settings consistent.
	db.SetMaxOpenConns(1)
	fail := func(err error) (*gorm.DB, *sql.DB, error) {
		db.Close()
		return nil, nil, err
	}
	if _, err := db.ExecContext(ctx, "PRAGMA busy_timeout = 5000"); err != nil {
		return fail(err)
	}
	orm, err := gorm.Open(gormsqlite.New(gormsqlite.Config{DriverName: "sqlite", Conn: db}), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Silent),
	})
	if err != nil {
		return fail(err)
	}
	return orm, db, nil
}
