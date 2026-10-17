package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"

	"github.com/ctailee/ctailee-backend/internal/config"
	"github.com/ctailee/ctailee-backend/internal/project/shorturl"
	"github.com/ctailee/ctailee-backend/internal/project/textcipher"
)

func main() {
	databasePath := "data/app.sqlite"
	if err := os.MkdirAll("data", 0755); err != nil {
		log.Fatalf("failed to create data directory: %v", err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	shortURLService, db, err := shorturl.Open(ctx, databasePath)
	cancel()
	if err != nil {
		log.Fatalf("failed to open short URL database: %v", err)
	}
	defer db.Close()

	r := gin.Default()
	r.Use(config.CORS())

	r.GET(
		"/health",
		func(c *gin.Context) {
			c.JSON(http.StatusOK, gin.H{
				"message": "let's go!",
			})
		})

	projectGroup := r.Group("/project")
	shorturl.RegisterRoutes(projectGroup, shortURLService)
	textcipher.RegisterRoutes(projectGroup)

	if err := r.Run(":8080"); err != nil {
		log.Fatalf("failed to run server: %v", err)
	}
}
