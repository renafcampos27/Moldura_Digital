param([Parameter(Mandatory=$true)][string]$ApkPath, [string]$Serial = '', [string]$SdkPath = "$env:LOCALAPPDATA\Android\Sdk")
$ErrorActionPreference = 'Stop'
$adbPath = Join-Path $SdkPath 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) { throw 'Indica um APK que tenhas gerado no Android Studio.' }
$adbArguments = @()
if ($Serial) { $adbArguments += @('-s', $Serial) }
& $adbPath @adbArguments install -r $ApkPath
if ($LASTEXITCODE -ne 0) { throw 'Falha na instalação. Verifica a ligação e a autorização de depuração.' }
