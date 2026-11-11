param([string]$Serial='emulator-5554')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot
$taskSdk=if($env:ANDROID_HOME) {$env:ANDROID_HOME} else {Join-Path $taskRoot '.tools\sdk'}
$taskAdb=Join-Path $taskSdk 'platform-tools\adb.exe'
if(-not (Test-Path -LiteralPath $taskAdb)) {throw 'Set ANDROID_HOME to your Android SDK.'}
$taskApk=Join-Path $taskRoot 'app\build\outputs\apk\debug\app-debug.apk'
$taskTestApk=Join-Path $taskRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
if(-not (Test-Path -LiteralPath $taskApk) -or -not (Test-Path -LiteralPath $taskTestApk)) {
    throw 'First run gradlew.bat assembleDebug assembleDebugAndroidTest, then start an API 29+ emulator.'
}
$taskDeviceState=& $taskAdb -s $Serial get-state
if($LASTEXITCODE -ne 0 -or $taskDeviceState -ne 'device') {throw "Device $Serial is not ready."}
& $taskAdb -s $Serial install -r $taskApk
if($LASTEXITCODE -ne 0) {throw 'App installation failed.'}
& $taskAdb -s $Serial install -r $taskTestApk
if($LASTEXITCODE -ne 0) {throw 'Test installation failed.'}
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'test-results') -Force | Out-Null
$taskResults=& $taskAdb -s $Serial shell am instrument -w -e class com.starfall.game.GameDeviceTest com.starfall.game.debug.test/androidx.test.runner.AndroidJUnitRunner
$taskTestExit=$LASTEXITCODE
$taskResults | Tee-Object -FilePath (Join-Path $taskRoot 'test-results\native-screenshot-tests.log')
if($taskTestExit -ne 0 -or ($taskResults -join "`n") -notmatch 'OK \(\d+ tests?\)') {
    throw 'Native tests did not pass. Review the log before using screenshots.'
}
$taskImages=Join-Path $taskRoot 'docs\screenshots\v0.3.0'
New-Item -ItemType Directory -Path $taskImages -Force | Out-Null
& $taskAdb -s $Serial pull '/sdcard/Android/data/com.starfall.game.debug/files/screenshots/v0.3.0/.' $taskImages
if($LASTEXITCODE -ne 0) {throw 'Screenshot download failed.'}
foreach($taskName in @('ship-vanguard','ship-falcon','ship-bastion','casual-combat','boss-combat','pause','debrief','victory')) {
    if(-not (Test-Path -LiteralPath (Join-Path $taskImages ($taskName+'.png')))) {throw "Missing screenshot: $taskName"}
}
Write-Output "Screenshots saved to $taskImages. Inspect them before adding them to the README."
