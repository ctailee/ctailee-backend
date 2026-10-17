# SQLite migrations

啟動應用程式時，goose 會依序執行尚未套用的 migration，並將版本記錄在
`goose_db_version`。SQL 檔案透過 `go:embed` 包進執行檔，部署時不需要另外複製。
GORM 僅負責資料存取，不再使用 `AutoMigrate`。

修改 schema 時：

1. 新增下一個版本，例如 `00002_add_short_url_description.sql`。
2. 在 `-- +goose Up` 後寫入變更 SQL，在 `-- +goose Down` 後寫入還原 SQL。
3. 同步修改 Go model，執行 `go test ./...`，重新編譯並啟動服務。

```sql
-- +goose Up
ALTER TABLE short_urls ADD COLUMN description TEXT;

-- +goose Down
ALTER TABLE short_urls DROP COLUMN description;
```

已套用的 migration 不會因檔案內容修改而重新執行；後續變更請新增版本。
啟動時只執行 Up，不會自動回滾；migration 失敗會中止啟動。

第一版允許沿用本專案先前由 GORM AutoMigrate 建立的 `short_urls` 表，
不刪除資料；全新的資料庫會直接建立表與唯一索引。
