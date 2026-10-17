# syntax=docker/dockerfile:1
FROM golang:1.27.2-alpine AS build

WORKDIR /src

COPY go.mod go.sum ./
RUN go mod download

COPY cmd ./cmd
COPY internal ./internal
COPY migrations ./migrations
RUN go build -o /out/server ./cmd

FROM alpine:3.23

WORKDIR /app
COPY --from=build /out/server ./server

ENV GIN_MODE=release

EXPOSE 8080

ENTRYPOINT ["/app/server"]
