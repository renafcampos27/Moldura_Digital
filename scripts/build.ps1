param([string]$JavaHome = '', [string]$SdkPath = '', [switch]$Release)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
elseif (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Android\Android Studio\jbr')) {
    $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
}
if ($SdkPath) {
    Set-Content -LiteralPath (Join-Path $projectPath 'local.properties') -Value ('sdk.dir=' + $SdkPath.Replace('\','/').Replace(':','\:')) -Encoding ascii
}
Push-Location $projectPath
try {
    # Both paths stop before APK generation/signing. R8 emits optimized DEX/resources only.
    if ($Release) {
        & .\gradlew.bat :app:minifyReleaseWithR8 :app:optimizeReleaseResources :app:lintRelease :app:testDebugUnitTest --console=plain
    } else {
        & .\gradlew.bat :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --console=plain
    }
    if ($LASTEXITCODE -ne 0) { throw 'A validação falhou. Consultar o resultado acima.' }
    Write-Output 'Validação concluída. O APK é gerado pelo utilizador no Android Studio.'
} finally { Pop-Location }
