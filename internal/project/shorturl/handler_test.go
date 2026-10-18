package shorturl

import (
	"bytes"
	"encoding/json"
	"errors"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
)

func TestRoutes(t *testing.T) {
	gin.SetMode(gin.TestMode)
	repo := &memoryRepository{entries: map[string]shortURL{}}
	router := gin.New()
	RegisterRoutes(router.Group("/project"), &Service{repository: repo, now: time.Now, generateCode: generateShortCode})
	call := func(method, path, body string, status int) *httptest.ResponseRecorder {
		t.Helper()
		request := httptest.NewRequest(method, "/project/shorturl/"+path, bytes.NewBufferString(body))
		request.Header.Set("Content-Type", "application/json")
		response := httptest.NewRecorder()
		router.ServeHTTP(response, request)
		if response.Code != status {
			t.Fatalf("%s %s: %d %s", method, path, response.Code, response.Body)
		}
		return response
	}
	response := call("POST", "shortenurl", `{"originalUrl":"https://ctailee.com/path?q=中文&x=1"}`, 200)
	var result struct {
		ShortCode string `json:"shortCode"`
	}
	if err := json.Unmarshal(response.Body.Bytes(), &result); err != nil {
		t.Fatal(err)
	}
	response = call("GET", result.ShortCode, "", 302)
	if response.Header().Get("Location") != "https://ctailee.com/path?q=%e4%b8%ad%e6%96%87&x=1" {
		t.Fatal(response.Header())
	}
	for _, body := range []string{`{}`, `{"originalUrl":" "}`, `{"originalUrl":123}`, `{`, `{"originalUrl":"javascript:alert(1)"}`, `{"originalUrl":"/relative"}`} {
		call("POST", "shortenurl", body, 400)
	}
	response = call("GET", "missing", "", 404)
	var apiError errorResponse
	if err := json.Unmarshal(response.Body.Bytes(), &apiError); err != nil {
		t.Fatal(err)
	}
	if apiError.Message != "Short code not found: missing" || apiError.Errors == nil {
		t.Fatal(apiError)
	}
	entry := repo.entries[result.ShortCode]
	past := time.Now().Add(-time.Hour)
	entry.ExpiresAt = &past
	repo.entries[result.ShortCode] = entry
	call("GET", result.ShortCode, "", 404)
	repo.err = errors.New("private database details")
	call("POST", "shortenurl", `{"originalUrl":"https://ctailee.com/"}`, 500)
	response = call("GET", result.ShortCode, "", 500)
	if bytes.Contains(response.Body.Bytes(), []byte(repo.err.Error())) {
		t.Fatal("leaked database error")
	}
}
