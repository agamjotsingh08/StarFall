param([switch]$DeviceTests)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot
$taskLocalJdk=Join-Path $taskRoot '.tools\jdk21'
if(Test-Path -LiteralPath $taskLocalJdk) {
    $taskJdk=Get-ChildItem -LiteralPath $taskLocalJdk -Directory | Select-Object -First 1
    if($taskJdk) { $env:JAVA_HOME=$taskJdk.FullName }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    throw 'Set JAVA_HOME to an installed JDK 21. See README.md.'
}
if(-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME=Join-Path $taskRoot '.tools\gradle-home' }
if(-not $env:ANDROID_HOME -and (Test-Path -LiteralPath "$taskRoot\.tools\sdk")) {
    $env:ANDROID_HOME=Join-Path $taskRoot '.tools\sdk'
}
& "$taskRoot\gradlew.bat" assembleDebug testDebugUnitTest lintDebug --console=plain
if($LASTEXITCODE -ne 0) {throw 'Build or validation failed.'}
New-Item -ItemType Directory -Path "$taskRoot\dist" -Force | Out-Null
Copy-Item -LiteralPath "$taskRoot\app\build\outputs\apk\debug\app-debug.apk" -Destination "$taskRoot\dist\starfall-debug.apk" -Force
if($DeviceTests) {
    & "$taskRoot\gradlew.bat" connectedDebugAndroidTest --console=plain
    if($LASTEXITCODE -ne 0) {throw 'Device tests failed.'}
}
