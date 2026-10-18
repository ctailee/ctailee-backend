# ctailee-backend

使用 Go、Gin 的後端 API。

## 使用 Docker Compose 在本機執行

### 前置準備

- 安裝並啟動 Docker（例如 Docker Desktop）。
- 安裝 Docker Compose v2，確認 `docker compose version` 可以執行。
- 確認本機的 `8080` port 未被其他服務占用。

以下指令皆在專案根目錄執行。本機使用 `compose.local.yaml`，會從原始碼建置
Docker image，不需要另外安裝 Go 或 SQLite。

### 啟動服務

```bash
docker compose -f compose.local.yaml up --build -d
```

各參數的用途：

- `-f compose.local.yaml`：指定要讀取的 Compose 設定檔。此處選擇本機建置用的
  `compose.local.yaml`；本專案不加 `-f` 時會使用 `compose.yaml`，改為拉取 GHCR image。
- `up`：建立並啟動服務。
- `--build`：啟動前先建置 image。
- `-d`：讓容器在背景執行。

後續的 `ps`、`logs`、`down` 也使用相同的 `-f compose.local.yaml`，以讀取同一份設定。
注意 `logs -f app` 中的 `-f` 是 `logs` 子指令的選項，表示持續追蹤新日誌，
與 `docker compose -f compose.local.yaml` 指定設定檔的用途不同。

第一次需要下載基底 image、Go 依賴並編譯，耗時會較長。

服務啟動後，可透過 `http://localhost:8080` 存取 API。
在瀏覽器開啟 `http://localhost:8080/health`，或執行：

```bash
curl --fail http://localhost:8080/health
```

正常回應：

```json
{"message":"ok"}
```

### 查看狀態與日誌

```bash
docker compose -f compose.local.yaml ps
docker compose -f compose.local.yaml logs -f app
```

查看日誌時按 `Ctrl+C` 可離開日誌畫面，背景服務仍會繼續執行。

### 修改程式後更新服務

```bash
docker compose -f compose.local.yaml up --build -d
```

原始碼沒有掛載進容器，因此修改程式後需要重新建置，不會自動熱重載。

### 停止服務

```bash
docker compose -f compose.local.yaml down
```

此指令會停止並移除容器及 Compose 網路，保留資料 volume。

### 資料保存

SQLite 資料庫位於容器的 `/app/data/app.sqlite`，透過 `sqlite-data` named volume
保存。啟動時會自動建立資料庫並套用尚未執行的 migration，不需要手動初始化。
重新建置容器或執行上述 `down` 指令，都會保留資料。

只有在確定要清空本機資料時，才執行：

```bash
docker compose -f compose.local.yaml down -v
```

這會刪除資料 volume，包含已儲存的短網址。

### 常見問題

- **8080 port 已被占用**：停止占用該 port 的服務，或將 `compose.local.yaml`
  的 ports 改成 `"8081:8080"`，再重新啟動，改用 `http://localhost:8081/health` 檢查。
- **服務啟動失敗**：執行 `docker compose -f compose.local.yaml logs --tail=100 app`
  查看錯誤訊息。
- **不小心使用 `docker compose up`**：未指定 `-f` 時會使用 `compose.yaml`，
  該檔案用於拉取 GHCR image；本機從原始碼建置請指定 `-f compose.local.yaml`。

兩份 Compose 設定使用相同的專案名稱 `ctailee-backend`，在同一台主機切換設定時，
會操作同一組服務及資料 volume。

## 使用 CI 發佈的 image

CI 以小寫的 GitHub repository 路徑作為 image 名稱；本專案為
`ghcr.io/ctailee/ctailee-backend`。每次發佈會推送 `latest` 和 commit SHA 前 7 碼
兩個 tag。`compose.yaml` 預設使用 `latest`：

```bash
docker compose pull
docker compose up -d --no-build
```

要指定某次發佈，可將以下範例的 `abcdef0` 換成該次 commit SHA 的前 7 碼，
並在兩個指令使用相同的 tag：

```bash
IMAGE_TAG=abcdef0 docker compose pull
IMAGE_TAG=abcdef0 docker compose up -d --no-build
```

若套件為私有，拉取前需使用具備套件讀取權限的帳號登入 GHCR。
