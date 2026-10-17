package shorturl

import (
	"context"
	"errors"
	"testing"
	"time"
)

type memoryRepository struct {
	entries map[string]shortURL
	err     error
}

func (r *memoryRepository) insert(_ context.Context, entry shortURL) error {
	if r.err != nil {
		return r.err
	}
	if _, exists := r.entries[entry.ShortCode]; exists {
		return errDuplicateCode
	}
	r.entries[entry.ShortCode] = entry
	return nil
}
func (r *memoryRepository) find(_ context.Context, code string) (shortURL, error) {
	if r.err != nil {
		return shortURL{}, r.err
	}
	entry, exists := r.entries[code]
	if !exists {
		return shortURL{}, ErrNotFound
	}
	return entry, nil
}
func (r *memoryRepository) deactivate(_ context.Context, code string) error {
	if r.err != nil {
		return r.err
	}
	entry := r.entries[code]
	entry.IsActive = false
	r.entries[code] = entry
	return nil
}

func TestServiceLifecycle(t *testing.T) {
	ctx := context.Background()
	repo := &memoryRepository{entries: map[string]shortURL{}}
	service := newService(repo)
	now := time.Date(2026, 10, 17, 12, 0, 0, 0, time.UTC)
	service.now = func() time.Time { return now }
	code, err := service.Create(ctx, "https://ctailee.com/")
	if err != nil {
		t.Fatal(err)
	}
	if len(code) != 7 {
		t.Fatalf("invalid code %q", code)
	}
	for _, c := range code {
		if !(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
			t.Fatalf("invalid code %q", code)
		}
	}
	entry := repo.entries[code]
	if !entry.IsActive || !entry.CreatedAt.Equal(now) || !entry.ExpiresAt.Equal(now.AddDate(0, 0, 3)) {
		t.Fatal(entry)
	}
	got, err := service.GetOriginalURL(ctx, code)
	if err != nil || got != entry.OriginalURL {
		t.Fatalf("get = %q, %v", got, err)
	}
	now = now.AddDate(0, 0, 4)
	if _, err := service.GetOriginalURL(ctx, code); !errors.Is(err, ErrNotFound) {
		t.Fatal(err)
	}
	if repo.entries[code].IsActive {
		t.Fatal("expired entry still active")
	}
	entry.ExpiresAt = nil
	entry.IsActive = true
	repo.entries[code] = entry
	if _, err := service.GetOriginalURL(ctx, code); err != nil {
		t.Fatal(err)
	}
	entry.IsActive = false
	repo.entries[code] = entry
	if _, err := service.GetOriginalURL(ctx, code); !errors.Is(err, ErrNotFound) {
		t.Fatal(err)
	}
	if _, err := service.GetOriginalURL(ctx, "missing"); !errors.Is(err, ErrNotFound) {
		t.Fatal(err)
	}
}

func TestCollisionAndStorageErrors(t *testing.T) {
	ctx := context.Background()
	repo := &memoryRepository{entries: map[string]shortURL{"exists1": {}}}
	service := newService(repo)
	attempts := 0
	service.generateCode = func() (string, error) {
		attempts++
		if attempts == 1 {
			return "exists1", nil
		}
		return "unique1", nil
	}
	code, err := service.Create(ctx, "https://ctailee.com/")
	if err != nil || code != "unique1" || attempts != 2 {
		t.Fatalf("create = %q, %v, attempts %d", code, err, attempts)
	}
	service.generateCode = func() (string, error) { return "exists1", nil }
	if _, err := service.Create(ctx, "https://ctailee.com/"); err == nil {
		t.Fatal("unbounded collision retry")
	}
	repo.err = errors.New("storage unavailable")
	if _, err := service.Create(ctx, "https://ctailee.com/"); !errors.Is(err, repo.err) {
		t.Fatal(err)
	}
	if _, err := service.GetOriginalURL(ctx, "unique1"); !errors.Is(err, repo.err) {
		t.Fatal(err)
	}
}
