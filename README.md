# 背單字小考 Android App

這是一個完整 Android Studio 專案雛形，使用原生 Android Java 製作，不使用 Compose，也不依賴第三方套件。

## 已完成功能

- 首頁顯示 20 個單字區塊。
- 每區設計為 500 個單字，未來可擴充到 20 區、10,000 個單字。
- 目前第 1 區內建 50 個測試單字。
- 點入第 1 區後會隨機打亂題目。
- 題目顯示英文單字。
- 下面顯示四個中文選項。
- 正確選項為該英文單字中文意思。
- 錯誤選項優先從相同詞性產生。
- 答對顯示「你是神吧！」。
- 答錯顯示「想想爸爸媽媽對你的栽培」。
- 一般測驗答錯會進入錯題複習區。
- 錯題複習區中，每題初始需要答對 5 次才會移出。
- 錯題複習答對會累積答對次數。
- 錯題複習答錯會讓該題需要答對的次數 +1。
- 使用 SharedPreferences 保存本機進度。
- 內建 GitHub Actions，可自動產生 Debug APK。

## Android Studio 開啟方式

1. 解壓縮本專案。
2. 用 Android Studio 選擇 `Open`。
3. 開啟 `VocabTrainerComplete` 資料夾。
4. 等待 Gradle Sync。
5. 按 Run 執行 App。

## 使用 GitHub Actions 產生 APK

1. 建立一個 GitHub repository。
2. 把本專案所有檔案推上 GitHub。
3. 到 GitHub 專案頁面，點選 `Actions`。
4. 選擇 `Build Android APK`。
5. 點選 `Run workflow`。
6. 執行完成後，到該次 workflow 頁面下載 artifact：`VocabTrainer-debug-apk`。

APK 會產生在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 未來正式版建議補強

- 把單字資料改成 JSON / CSV 匯入。
- 正式改用 Room Database 儲存單字與學習紀錄。
- 增加每區完成度與正確率統計。
- 增加每日複習目標。
- 增加搜尋單字功能。
- 增加單字發音。
- 增加匯出 / 備份進度。
