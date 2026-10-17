package shorturl

import (
	"context"
	"database/sql"
	"errors"

	gormsqlite "gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
	"modernc.org/sqlite"

	"github.com/ctailee/ctailee-backend/migrations"
)

type sqliteRepository struct{ db *gorm.DB }

// Open opens SQLite and applies pending versioned SQL migrations.
// The caller closes the returned connection pool.
func Open(ctx context.Context, path string) (*Service, *sql.DB, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return nil, nil, err
	}
	// A single connection serializes writes and keeps connection settings consistent.
	db.SetMaxOpenConns(1)
	fail := func(err error) (*Service, *sql.DB, error) { db.Close(); return nil, nil, err }
	if _, err := db.ExecContext(ctx, "PRAGMA busy_timeout = 5000"); err != nil {
		return fail(err)
	}
	orm, err := gorm.Open(gormsqlite.New(gormsqlite.Config{DriverName: "sqlite", Conn: db}), &gorm.Config{Logger: logger.Default.LogMode(logger.Silent)})
	if err != nil {
		return fail(err)
	}
	if err := migrations.Up(ctx, db); err != nil {
		return fail(err)
	}

	return newService(&sqliteRepository{db: orm}), db, nil
}

func (r *sqliteRepository) insert(ctx context.Context, entry shortURL) error {
	entry.ID = 0
	err := r.db.WithContext(ctx).Create(&entry).Error
	var sqliteError *sqlite.Error
	if errors.As(err, &sqliteError) && sqliteError.Code() == 2067 {
		return errDuplicateCode
	}
	return err
}

func (r *sqliteRepository) find(ctx context.Context, code string) (shortURL, error) {
	var entry shortURL
	err := r.db.WithContext(ctx).Where("short_code = ?", code).Take(&entry).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return shortURL{}, ErrNotFound
	}
	return entry, err
}

func (r *sqliteRepository) deactivate(ctx context.Context, code string) error {
	return r.db.WithContext(ctx).Model(&shortURL{}).Where("short_code = ?", code).Update("is_active", false).Error
}
