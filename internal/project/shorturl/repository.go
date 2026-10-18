package shorturl

import (
	"context"
	"errors"

	"gorm.io/gorm"
	"modernc.org/sqlite"
)

type repository interface {
	insert(context.Context, shortURL) error
	find(context.Context, string) (shortURL, error)
	deactivate(context.Context, string) error
}

type sqliteRepository struct{ db *gorm.DB }

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
