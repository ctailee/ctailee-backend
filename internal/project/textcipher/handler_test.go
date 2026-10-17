package textcipher

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
)

func TestRoutes(t *testing.T) {
	gin.SetMode(gin.TestMode)
	router := gin.New()
	RegisterRoutes(router.Group("/project"))
	post := func(path, body string, status int) map[string]any {
		t.Helper()
		request := httptest.NewRequest(http.MethodPost, "/project/textcipher/"+path, bytes.NewBufferString(body))
		request.Header.Set("Content-Type", "application/json")
		response := httptest.NewRecorder()
		router.ServeHTTP(response, request)
		if response.Code != status {
			t.Fatalf("%s: status %d: %s", path, response.Code, response.Body)
		}
		var result map[string]any
		if err := json.Unmarshal(response.Body.Bytes(), &result); err != nil {
			t.Fatal(err)
		}
		return result
	}
	encrypted := post("encrypt", `{"password":"secret","plainText":"中文 🔐"}`, 200)
	body, _ := json.Marshal(map[string]any{"password": "secret", "encryptedText": encrypted["encryptedText"]})
	decrypted := post("decrypt", string(body), 200)
	if decrypted["plainText"] != "中文 🔐" {
		t.Fatal(decrypted)
	}
	for _, path := range []string{"encrypt", "decrypt"} {
		result := post(path, `{"password":" "}`, 400)
		if result["message"] != "Request validation failed" || len(result["errors"].(map[string]any)) != 2 {
			t.Fatal(result)
		}
		post(path, `{"password":123}`, 400)
		post(path, `{`, 400)
	}
	result := post("decrypt", `{"password":"secret","encryptedText":"invalid"}`, 400)
	if result["message"] != "Unable to process the encrypted text" {
		t.Fatal(result)
	}
	body, _ = json.Marshal(map[string]any{"password": "wrong", "encryptedText": encrypted["encryptedText"]})
	post("decrypt", string(body), 400)
}
