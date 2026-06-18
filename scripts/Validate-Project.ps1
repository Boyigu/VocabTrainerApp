param(
    [string]$Root = (Resolve-Path "$PSScriptRoot\..")
)

$ErrorActionPreference = "Stop"

function Read-Utf8Text {
    param([string]$Path)

    $fullPath = Join-Path $Root $Path
    return [System.IO.File]::ReadAllText($fullPath, [System.Text.Encoding]::UTF8)
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$requiredUtf8Files = @(
    "README.md",
    "app\src\main\java\com\example\vocabtrainer\MainActivity.java",
    "app\src\main\assets\vocab_500_words.json",
    "app\src\main\res\values\strings.xml"
)

foreach ($path in $requiredUtf8Files) {
    $text = Read-Utf8Text $path
    Assert-True (-not $text.Contains([char]0xFFFD)) "$path contains invalid UTF-8 replacement characters."
}

$stringsXml = Read-Utf8Text "app\src\main\res\values\strings.xml"
[xml]$stringsXml | Out-Null

$vocabJson = Read-Utf8Text "app\src\main\assets\vocab_500_words.json"
$words = $vocabJson | ConvertFrom-Json
Assert-True ($words.Count -ge 100) "Expected at least 100 vocabulary words."

$mainActivity = Read-Utf8Text "app\src\main\java\com\example\vocabtrainer\MainActivity.java"
$lineNumber = 0
foreach ($line in ($mainActivity -split "`r?`n")) {
    $lineNumber++
    $quoteCount = ([regex]::Matches($line, '"')).Count
    Assert-True (($quoteCount % 2) -eq 0) "Odd number of quotes in MainActivity.java line $lineNumber."
}

$workflow = Read-Utf8Text ".github\workflows\build-apk.yml"
Assert-True ($workflow -match "gradle/actions/setup-gradle@") "GitHub Actions must install/configure Gradle explicitly."
Assert-True ($workflow -match "gradle-version:") "GitHub Actions must pin a Gradle version."
Assert-True ($workflow -match "(?m)^\s*run:\s*gradle assembleDebug\s*$") "GitHub Actions must build with the configured Gradle executable."

$gradlewPath = Join-Path $Root "gradlew"
$wrapperJarPath = Join-Path $Root "gradle\wrapper\gradle-wrapper.jar"
$wrapperPropertiesPath = Join-Path $Root "gradle\wrapper\gradle-wrapper.properties"
if (Test-Path $gradlewPath) {
    Assert-True ((Test-Path $wrapperJarPath) -and (Test-Path $wrapperPropertiesPath)) "Do not keep a fake gradlew without Gradle wrapper files."
}

Write-Host "Project validation passed."
