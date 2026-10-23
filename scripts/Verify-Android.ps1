param([switch]$Instrumented)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$studioJdk = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
if (!$env:JAVA_HOME -and (Test-Path -LiteralPath $studioJdk)) { $env:JAVA_HOME = $studioJdk }
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Falló la comprobación Android. Revisa el error de Gradle.' }
    if ($Instrumented) {
        & .\gradlew.bat :app:connectedDebugAndroidTest --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de Compose. Comprueba el dispositivo.' }
    }
    Write-Output 'Verificación Android completada correctamente.'
} finally { Pop-Location }
