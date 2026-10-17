package config

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
)

func TestCORS(t *testing.T) {
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.Use(CORS())
	router.POST("/project/textcipher/encrypt", func(c *gin.Context) { c.Status(http.StatusOK) })
	for _, tc := range []struct {
		origin string
		method string
		status int
	}{
		{"https://ctailee.com", "OPTIONS", 204},
		{"http://localhost:5173", "OPTIONS", 204},
		{"http://127.0.0.1:5173", "OPTIONS", 204},
		{"https://ctailee.com", "POST", 200},
		{"https://untrusted.example", "OPTIONS", 403},
		{"https://ctailee.com.evil.example", "POST", 403},
		{"", "POST", 200},
	} {
		t.Run(tc.origin+tc.method, func(t *testing.T) {
			request := httptest.NewRequest(tc.method, "/project/textcipher/encrypt", nil)
			if tc.origin != "" {
				request.Header.Set("Origin", tc.origin)
			}
			if tc.method == "OPTIONS" {
				request.Header.Set("Access-Control-Request-Method", "POST")
				request.Header.Set("Access-Control-Request-Headers", "Content-Type")
			}
			response := httptest.NewRecorder()
			router.ServeHTTP(response, request)
			if response.Code != tc.status {
				t.Fatalf("status = %d", response.Code)
			}
			allowed := response.Header().Get("Access-Control-Allow-Origin")
			if tc.status < 400 && tc.origin != "" {
				if allowed != tc.origin {
					t.Fatalf("allowed origin = %q", allowed)
				}
				if tc.method == "OPTIONS" && response.Header().Get("Access-Control-Allow-Methods") == "" {
					t.Fatal("missing preflight methods")
				}
			} else if allowed != "" {
				t.Fatalf("unexpected allowed origin %q", allowed)
			}
		})
	}
}
