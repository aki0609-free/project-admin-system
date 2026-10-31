@echo off
setlocal
chcp 65001 >nul

rem ============================================================
rem ProjectAdmin desktop shortcut settings
rem ============================================================
set "APP_URL=https://project-admin.fuyo-system.com/"
set "SHORTCUT_NAME=ProjectAdmin"
set "ICON_URL=%APP_URL%favicon.ico"
set "SCRIPT_DIR=%~dp0"

echo ProjectAdmin のデスクトップショートカットを作成します。

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$desktop = [Environment]::GetFolderPath('Desktop');" ^
  "$appDirectory = Join-Path $env:LOCALAPPDATA 'ProjectAdmin';" ^
  "$installedIcon = Join-Path $appDirectory 'project-admin.ico';" ^
  "$customIcon = Join-Path $env:SCRIPT_DIR 'project-admin.ico';" ^
  "$repositoryIcon = Join-Path $env:SCRIPT_DIR '..\..\..\frontend\public\favicon.ico';" ^
  "$iconFile = Join-Path $env:SystemRoot 'System32\SHELL32.dll';" ^
  "$iconIndex = 220;" ^
  "New-Item -ItemType Directory -Path $appDirectory -Force | Out-Null;" ^
  "if (Test-Path -LiteralPath $customIcon) {" ^
  "  Copy-Item -LiteralPath $customIcon -Destination $installedIcon -Force;" ^
  "  $iconFile = $installedIcon;" ^
  "  $iconIndex = 0;" ^
  "} elseif (Test-Path -LiteralPath $repositoryIcon) {" ^
  "  Copy-Item -LiteralPath $repositoryIcon -Destination $installedIcon -Force;" ^
  "  $iconFile = $installedIcon;" ^
  "  $iconIndex = 0;" ^
  "} else {" ^
  "  $temporaryIcon = Join-Path $env:TEMP 'project-admin-shortcut.ico';" ^
  "  try {" ^
  "    Invoke-WebRequest -UseBasicParsing -Uri $env:ICON_URL -OutFile $temporaryIcon | Out-Null;" ^
  "    $iconBytes = [IO.File]::ReadAllBytes($temporaryIcon);" ^
  "    if ($iconBytes.Length -lt 4 -or $iconBytes[0] -ne 0 -or $iconBytes[1] -ne 0 -or $iconBytes[2] -ne 1 -or $iconBytes[3] -ne 0) {" ^
  "      throw '取得したファイルはICO形式ではありません。';" ^
  "    }" ^
  "    Move-Item -LiteralPath $temporaryIcon -Destination $installedIcon -Force;" ^
  "    $iconFile = $installedIcon;" ^
  "    $iconIndex = 0;" ^
  "  } catch {" ^
  "    Remove-Item -LiteralPath $temporaryIcon -Force -ErrorAction SilentlyContinue;" ^
  "    Write-Warning '専用アイコンを取得できなかったため、Windows標準アイコンを使用します。';" ^
  "  }" ^
  "}" ^
  "$shortcutPath = Join-Path $desktop ($env:SHORTCUT_NAME + '.url');" ^
  "$shortcut = @(" ^
  "  '[InternetShortcut]'," ^
  "  ('URL=' + $env:APP_URL)," ^
  "  ('IconFile=' + $iconFile)," ^
  "  ('IconIndex=' + $iconIndex)" ^
  ");" ^
  "[IO.File]::WriteAllLines($shortcutPath, $shortcut, [Text.Encoding]::ASCII);" ^
  "Write-Host ('作成しました: ' + $shortcutPath);"

if errorlevel 1 (
  echo.
  echo ショートカットの作成に失敗しました。
  pause
  exit /b 1
)

echo.
echo 作成完了です。デスクトップの ProjectAdmin をダブルクリックしてください。
pause
endlocal
