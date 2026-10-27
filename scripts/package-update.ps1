[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+(\.\d+){1,2}$')]
    [string]$VersionName,

    [string]$Changelog = ''
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$gradleFile = Join-Path $repositoryRoot 'app\build.gradle.kts'
$buildFile = Join-Path $repositoryRoot 'app\build\outputs\apk\debug\app-debug.apk'
$distributionDirectory = Join-Path $repositoryRoot 'dist'
$manifestDraftFile = Join-Path $distributionDirectory 'update.json'

$originalGradle = [System.IO.File]::ReadAllText($gradleFile)
$versionCodePattern = '(?m)^([ \t]*versionCode[ \t]*=[ \t]*)(\d+)([ \t]*\r?)$'
$versionNamePattern = '(?m)^([ \t]*versionName[ \t]*=[ \t]*)"([^"]*)"([ \t]*\r?)$'
$versionCodeMatches = [regex]::Matches($originalGradle, $versionCodePattern)
$versionNameMatches = [regex]::Matches($originalGradle, $versionNamePattern)

if ($versionCodeMatches.Count -ne 1 -or $versionNameMatches.Count -ne 1) {
    throw 'Could not find exactly one versionCode and versionName in app/build.gradle.kts.'
}

$currentVersionName = $versionNameMatches[0].Groups[2].Value
$currentVersionCode = [long]$versionCodeMatches[0].Groups[2].Value
if ($VersionName -eq $currentVersionName) {
    $nextVersionCode = $currentVersionCode
    $updatedGradle = $originalGradle
}
else {
    $nextVersionCode = $currentVersionCode + 1
    $updatedGradle = [regex]::Replace(
        $originalGradle,
        $versionCodePattern,
        ('$1' + $nextVersionCode + '$3'),
        1
    )
    $updatedGradle = [regex]::Replace(
        $updatedGradle,
        $versionNamePattern,
        ('$1"' + $VersionName + '"$3'),
        1
    )
}

$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)
if ($updatedGradle -ne $originalGradle) {
    [System.IO.File]::WriteAllText($gradleFile, $updatedGradle, $utf8WithoutBom)
}

if ([string]::IsNullOrWhiteSpace($Changelog)) {
    $Changelog = "event $VersionName"
}

try {
    Push-Location $repositoryRoot
    try {
        & .\gradlew.bat :app:assembleDebug
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}
catch {
    [System.IO.File]::WriteAllText($gradleFile, $originalGradle, $utf8WithoutBom)
    throw
}

if (-not (Test-Path -LiteralPath $buildFile)) {
    throw "Build completed but the APK was not found at $buildFile."
}

New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null
$apkFile = Join-Path $distributionDirectory "event-$VersionName.apk"
Copy-Item -LiteralPath $buildFile -Destination $apkFile -Force
$sha256 = (Get-FileHash -LiteralPath $apkFile -Algorithm SHA256).Hash.ToLowerInvariant()

$manifest = [ordered]@{
    versionCode = $nextVersionCode
    versionName = $VersionName
    apkUrl = ''
    sha256 = $sha256
    changelog = $Changelog
}
$manifestJson = $manifest | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($manifestDraftFile, ($manifestJson + [Environment]::NewLine), $utf8WithoutBom)

Write-Host ''
Write-Host "Built APK: $apkFile"
Write-Host "Version: $VersionName (versionCode $nextVersionCode)"
Write-Host "SHA-256: $sha256"
Write-Host "Manifest draft: $manifestDraftFile"
Write-Host 'Next: upload the APK as a GitHub Release attachment, fill apkUrl in the draft, then copy it to updates/update.json and publish that change on main.'
