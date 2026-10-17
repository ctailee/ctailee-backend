package shorturl

import (
	"context"
	"crypto/rand"
	"errors"
	"math/big"
	"time"
)

var (
	ErrNotFound      = errors.New("short code not found")
	errDuplicateCode = errors.New("duplicate short code")
)

type shortURL struct {
	ID          uint   `gorm:"primaryKey"`
	OriginalURL string `gorm:"column:original_url;not null"`
	ShortCode   string `gorm:"not null;uniqueIndex"`
	CreatedAt   time.Time
	ExpiresAt   *time.Time
	IsActive    bool
}

func (shortURL) TableName() string { return "short_urls" }

type repository interface {
	insert(context.Context, shortURL) error
	find(context.Context, string) (shortURL, error)
	deactivate(context.Context, string) error
}

type Service struct {
	repository   repository
	now          func() time.Time
	generateCode func() (string, error)
}

func newService(repository repository) *Service {
	return &Service{repository: repository, now: time.Now, generateCode: generateShortCode}
}

func (s *Service) Create(ctx context.Context, originalURL string) (string, error) {
	for attempt := 0; attempt < 10; attempt++ {
		code, err := s.generateCode()
		if err != nil {
			return "", err
		}
		now := s.now()
		expiresAt := now.AddDate(0, 0, 3)
		err = s.repository.insert(ctx, shortURL{OriginalURL: originalURL, ShortCode: code, CreatedAt: now, ExpiresAt: &expiresAt, IsActive: true})
		if errors.Is(err, errDuplicateCode) {
			continue
		}
		if err != nil {
			return "", err
		}
		return code, nil
	}
	return "", errors.New("unable to generate a unique short code")
}

func (s *Service) GetOriginalURL(ctx context.Context, code string) (string, error) {
	entry, err := s.repository.find(ctx, code)
	if err != nil {
		return "", err
	}
	if entry.ExpiresAt != nil && entry.ExpiresAt.Before(s.now()) {
		if err := s.repository.deactivate(ctx, code); err != nil {
			return "", err
		}
		return "", ErrNotFound
	}
	if !entry.IsActive {
		return "", ErrNotFound
	}
	return entry.OriginalURL, nil
}

func generateShortCode() (string, error) {
	const characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
	var code [7]byte
	for i := range code {
		index, err := rand.Int(rand.Reader, big.NewInt(int64(len(characters))))
		if err != nil {
			return "", err
		}
		code[i] = characters[index.Int64()]
	}
	return string(code[:]), nil
}
