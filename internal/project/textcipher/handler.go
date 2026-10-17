package textcipher

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

type cipherRequest struct {
	Password      string `json:"password"`
	PlainText     string `json:"plainText"`
	EncryptedText string `json:"encryptedText"`
}

type errorResponse struct {
	Message string            `json:"message"`
	Errors  map[string]string `json:"errors"`
}

func encrypt(c *gin.Context) { handleCipher(c, true) }
func decrypt(c *gin.Context) { handleCipher(c, false) }

func handleCipher(c *gin.Context, encrypting bool) {
	var request cipherRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, errorResponse{"Invalid request body", map[string]string{}})
		return
	}
	field, value, message := "encryptedText", request.EncryptedText, "encrypted text must not be blank"
	if encrypting {
		field, value, message = "plainText", request.PlainText, "plain text must not be blank"
	}
	validationErrors := map[string]string{}
	if strings.TrimSpace(request.Password) == "" {
		validationErrors["password"] = "password must not be blank"
	}
	if strings.TrimSpace(value) == "" {
		validationErrors[field] = message
	}
	if len(validationErrors) > 0 {
		c.JSON(http.StatusBadRequest, errorResponse{"Request validation failed", validationErrors})
		return
	}
	var result string
	var err error
	if encrypting {
		result, err = Encrypt(request.Password, value)
	} else {
		result, err = Decrypt(request.Password, value)
	}
	if err != nil {
		status := http.StatusBadRequest
		if encrypting {
			status = http.StatusInternalServerError
		}
		c.JSON(status, errorResponse{"Unable to process the encrypted text", map[string]string{}})
		return
	}
	responseField := "plainText"
	if encrypting {
		responseField = "encryptedText"
	}
	c.JSON(http.StatusOK, gin.H{responseField: result})
}
