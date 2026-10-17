package shorturl

import (
	"errors"
	"net/http"
	"net/url"
	"strings"

	"github.com/gin-gonic/gin"
)

type handler struct{ service *Service }

type errorResponse struct {
	Message string            `json:"message"`
	Errors  map[string]string `json:"errors"`
}

func (h handler) shorten(c *gin.Context) {
	var request struct {
		OriginalURL string `json:"originalUrl"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, errorResponse{"Invalid request body", map[string]string{}})
		return
	}
	validationErrors := map[string]string{}
	if strings.TrimSpace(request.OriginalURL) == "" {
		validationErrors["originalUrl"] = "Original URL can't be empty"
	} else {
		parsed, err := url.Parse(request.OriginalURL)
		if err != nil || parsed.Host == "" || (parsed.Scheme != "http" && parsed.Scheme != "https") {
			validationErrors["originalUrl"] = "Original URL must be an absolute HTTP or HTTPS URL"
		}
	}
	if len(validationErrors) > 0 {
		c.JSON(http.StatusBadRequest, errorResponse{"Request validation failed", validationErrors})
		return
	}
	code, err := h.service.Create(c.Request.Context(), request.OriginalURL)
	if err != nil {
		c.JSON(http.StatusInternalServerError, errorResponse{"Unable to process the short URL", map[string]string{}})
		return
	}
	c.JSON(http.StatusOK, gin.H{"shortCode": code})
}

func (h handler) redirect(c *gin.Context) {
	code := c.Param("shortCode")
	originalURL, err := h.service.GetOriginalURL(c.Request.Context(), code)
	if errors.Is(err, ErrNotFound) {
		c.JSON(http.StatusNotFound, errorResponse{"Short code not found: " + code, map[string]string{}})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, errorResponse{"Unable to process the short URL", map[string]string{}})
		return
	}
	c.Redirect(http.StatusFound, originalURL)
}
