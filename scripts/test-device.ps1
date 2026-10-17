param([string]$Serial = '', [string]$SdkPath = "$env:LOCALAPPDATA\Android\Sdk")
$ErrorActionPreference = 'Stop'
$adbPath = Join-Path $SdkPath 'platform-tools\adb.exe'
$adbArguments = @()
if ($Serial) { $adbArguments += @('-s', $Serial) }
# Requer a aplicação e o pacote de testes previamente instalados pelo utilizador.
# Não chama o Gradle e não gera nem instala APKs.
$result = & $adbPath @adbArguments shell am instrument -w -r pt.molduratv.test/androidx.test.runner.AndroidJUnitRunner 2>&1
$exitCode = $LASTEXITCODE
$result | Write-Output
$resultText = $result -join "`n"
if ($exitCode -ne 0 -or $resultText -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
    throw 'Os testes falharam ou os pacotes não estão instalados. Prepara-os no Android Studio.'
}
if ($resultText -notmatch 'OK \((\d+) tests?\)' -or [int]$Matches[1] -lt 7) {
    throw 'A bateria de testes ficou incompleta. Verifica a execução no Android Studio.'
}
