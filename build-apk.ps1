# Builds and signs the APK straight from the SDK tools (no Gradle daemon needed).
# Usage: .\build-apk.ps1 [-Install]
param([switch]$Install)
$ErrorActionPreference = 'Stop'

$sdk   = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { 'C:\Android\sdk' }
$bt    = "$sdk\build-tools\35.0.0"
$jar   = "$sdk\platforms\android-36\android.jar"
$pkg   = 'com.abbysimple.worldclock'
$src   = "$PSScriptRoot\app\src\main"
$out   = "$PSScriptRoot\build\raw"
$apk   = "$PSScriptRoot\build\WorldClockWidget.apk"

function Run($exe) {
    & $exe @args
    if ($LASTEXITCODE -ne 0) { throw "$(Split-Path $exe -Leaf) failed ($LASTEXITCODE)" }
}

Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$out\gen", "$out\classes", "$out\dex" | Out-Null

# The manifest leaves the package to Gradle's `namespace`, so add it for aapt2.
(Get-Content "$src\AndroidManifest.xml" -Raw) -replace '<manifest ', "<manifest package=`"$pkg`" " |
    Set-Content -Encoding utf8 "$out\AndroidManifest.xml"

Run "$bt\aapt2.exe" compile --dir "$src\res" -o "$out\res.zip"
Run "$bt\aapt2.exe" link -I $jar --manifest "$out\AndroidManifest.xml" `
    --min-sdk-version 26 --target-sdk-version 35 --version-code 1 --version-name 1.0 `
    --java "$out\gen" -o "$out\unsigned.apk" "$out\res.zip"

$javaFiles = Get-ChildItem -Recurse "$src\java", "$out\gen" -Filter *.java | ForEach-Object FullName
Run javac --release 11 -encoding UTF-8 -classpath $jar -d "$out\classes" @javaFiles
$classFiles = Get-ChildItem -Recurse "$out\classes" -Filter *.class | ForEach-Object FullName
Run "$bt\d8.bat" --min-api 26 --lib $jar --output "$out\dex" @classFiles

Push-Location "$out\dex"
try { Run jar -u -f "$out\unsigned.apk" -0 -M classes.dex } finally { Pop-Location }

New-Item -ItemType Directory -Force (Split-Path $apk) | Out-Null
Run "$bt\zipalign.exe" -f -p 4 "$out\unsigned.apk" "$out\aligned.apk"
Run "$bt\apksigner.bat" sign --ks "$env:USERPROFILE\.android\debug.keystore" `
    --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android `
    --out $apk "$out\aligned.apk"
Write-Host "Built $apk"

if ($Install) { Run adb install -r $apk }
