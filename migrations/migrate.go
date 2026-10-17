package migrations

import (
	"context"
	"database/sql"
	"embed"
	"io/fs"

	"github.com/pressly/goose/v3"
)

//go:embed *.sql
var files embed.FS

// Up applies pending SQL migrations embedded in the binary.
func Up(ctx context.Context, db *sql.DB) error {
	return up(ctx, db, files)
}

func up(ctx context.Context, db *sql.DB, files fs.FS) error {
	provider, err := goose.NewProvider(goose.DialectSQLite3, db, files, goose.WithDisableGlobalRegistry(true))
	if err != nil {
		return err
	}
	_, err = provider.Up(ctx)
	return err
}
