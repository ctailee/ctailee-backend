package shorturl

import "github.com/gin-gonic/gin"

func RegisterRoutes(r *gin.RouterGroup, service *Service) {
	h := handler{service: service}
	group := r.Group("/shorturl")
	group.POST("/shortenurl", h.shorten)
	group.GET("/:shortCode", h.redirect)
}
