package textcipher

import "github.com/gin-gonic/gin"

func RegisterRoutes(r *gin.RouterGroup) {
	textCipherGroup := r.Group("/textcipher")
	textCipherGroup.POST("/encrypt", encrypt)
	textCipherGroup.POST("/decrypt", decrypt)
}
