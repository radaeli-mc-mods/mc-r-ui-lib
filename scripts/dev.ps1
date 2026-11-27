param(
    [string]$Task = 'buildAll',
    [switch]$Validate,
    [switch]$WithJei,
    [string]$Java17 = $env:R_UI_JAVA17,
    [string]$Java21 = $env:R_UI_JAVA21,
    [string]$GradleHome = $env:R_UI_GRADLE_HOME
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
$validationLoader = $null
if ($Validate) {
    if ($Task -match '^(:forge:runClient|runForgeClient)$') { $validationLoader = 'forge' }
    elseif ($Task -match '^(:neoforge:runClient|runNeoForgeClient|runClient)$') { $validationLoader = 'neoforge' }
    else { throw '-Validate requires a client run task.' }
}

function Find-Jdk([int]$Major, [string]$ExplicitPath) {
    $candidates = @($ExplicitPath, $env:JAVA_HOME)
    foreach ($jdkRoot in @("$env:USERPROFILE/.jdks", "$env:USERPROFILE/.gradle/jdks", "$env:ProgramFiles/Eclipse Adoptium", "$env:ProgramFiles/Java")) {
        if (Test-Path -LiteralPath $jdkRoot) {
            $candidates += @(Get-ChildItem -LiteralPath $jdkRoot -Directory | Select-Object -ExpandProperty FullName)
        }
    }
    foreach ($candidate in $candidates) {
        if (!$candidate -or !(Test-Path -LiteralPath "$candidate/release")) { continue }
        $release = Get-Content -LiteralPath "$candidate/release" -Raw
        if ($release -match "JAVA_VERSION=`"$Major[\.\+`"]") { return (Resolve-Path -LiteralPath $candidate).Path }
    }
    throw "JDK $Major not found. Install Temurin $Major or provide -Java$Major / R_UI_JAVA$Major."
}

$jdk17Path = Find-Jdk 17 $Java17
$jdk21Path = Find-Jdk 21 $Java21
$env:JAVA_HOME = $jdk21Path
if (!$GradleHome) { $GradleHome = Join-Path $projectRoot '.gradle-r-ui-lib' }
$env:GRADLE_USER_HOME = $GradleHome
$gradleArguments = @($Task, '--no-daemon', '--no-parallel', '--console=plain', "-Dorg.gradle.java.installations.paths=$jdk17Path,$jdk21Path")
if ($Validate) { $gradleArguments += '-PuiValidation' }
if ($WithJei) { $gradleArguments += '-PwithJei' }
$runStarted = [DateTime]::UtcNow
Push-Location $projectRoot
try {
    & ./gradlew.bat @gradleArguments
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Validate) {
        $resultPath = Join-Path $projectRoot "$validationLoader/runs/validation/ui-validation/results.json"
        if (!(Test-Path -LiteralPath $resultPath) -or (Get-Item -LiteralPath $resultPath).LastWriteTimeUtc -lt $runStarted) {
            throw 'The client exited without producing a new validation result.'
        }
        if (!(Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json).passed) { throw 'Runtime validation failed.' }
    }
    exit $LASTEXITCODE
} finally { Pop-Location }
