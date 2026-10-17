package textcipher

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"strconv"
	"strings"
)

const tokenHeader = "pte1.pbkdf2-sha256.600000"

var errInvalidToken = errors.New("unable to process the encrypted text")

// Encrypt produces a token compatible with the Java text cipher.
func Encrypt(password, plainText string) (string, error) {
	if password == "" {
		return "", errors.New("password must not be empty")
	}
	salt := make([]byte, 16)
	iv := make([]byte, 12)
	if _, err := rand.Read(salt); err != nil {
		return "", err
	}
	if _, err := rand.Read(iv); err != nil {
		return "", err
	}
	gcm, err := newGCM(password, salt, 600000)
	if err != nil {
		return "", err
	}
	encrypted := gcm.Seal(nil, iv, []byte(plainText), []byte(tokenHeader))
	encode := base64.RawURLEncoding.EncodeToString
	return strings.Join([]string{tokenHeader, encode(salt), encode(iv), encode(encrypted)}, "."), nil
}

// Decrypt authenticates the token header and content before returning plaintext.
func Decrypt(password, encryptedText string) (string, error) {
	if password == "" {
		return "", errors.New("password must not be empty")
	}
	parts := strings.Split(encryptedText, ".")
	if len(parts) != 6 || parts[0] != "pte1" || parts[1] != "pbkdf2-sha256" {
		return "", errInvalidToken
	}
	iterations, err := strconv.Atoi(parts[2])
	if err != nil || iterations <= 0 || iterations > 10000000 {
		return "", errInvalidToken
	}
	fields := make([][]byte, 3)
	for i := range fields {
		encoding := base64.RawURLEncoding
		if strings.Contains(parts[i+3], "=") {
			encoding = base64.URLEncoding
		}
		// Java's URL decoder rejects whitespace, unlike Go's decoder.
		if strings.ContainsAny(parts[i+3], "\r\n") {
			return "", errInvalidToken
		}
		fields[i], err = encoding.DecodeString(parts[i+3])
		if err != nil {
			return "", errInvalidToken
		}
	}
	if len(fields[0]) != 16 || len(fields[1]) != 12 || len(fields[2]) < 16 {
		return "", errInvalidToken
	}
	gcm, err := newGCM(password, fields[0], iterations)
	if err != nil {
		return "", err
	}
	plaintext, err := gcm.Open(nil, fields[1], fields[2], []byte(strings.Join(parts[:3], ".")))
	if err != nil {
		return "", errInvalidToken
	}
	return string(plaintext), nil
}

func newGCM(password string, salt []byte, iterations int) (cipher.AEAD, error) {
	key, err := pbkdf2.Key(sha256.New, password, salt, iterations, 32)
	if err != nil {
		return nil, err
	}
	defer clear(key)
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	return cipher.NewGCM(block)
}
