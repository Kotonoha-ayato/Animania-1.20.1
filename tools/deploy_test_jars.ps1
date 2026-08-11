param(
    [Parameter(Mandatory = $true)][string]$BuildRoot,
    [Parameter(Mandatory = $true)][string]$ModsDirectory
)

$ErrorActionPreference = 'Stop'
$mods = [IO.Path]::GetFullPath($ModsDirectory)
$gameDirectory = [IO.Directory]::GetParent($mods).FullName
$running = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -in @('java.exe', 'javaw.exe') -and
    $_.CommandLine -and $_.CommandLine.IndexOf($gameDirectory, [StringComparison]::OrdinalIgnoreCase) -ge 0
}
if ($running) {
    throw "Refusing to replace loaded mod JARs while Minecraft is running for $gameDirectory. Exit Minecraft completely and retry."
}

$jars = @(
    'base/build/release/animania-base-1.20.1-3.0.0.jar',
    'farm/build/release/animania-farm-1.20.1-3.0.0.jar',
    'extra/build/release/animania-extra-1.20.1-3.0.0.jar',
    'catsdogs/build/release/animania-catsdogs-1.20.1-3.0.0.jar'
)
$sources = $jars | ForEach-Object { [IO.Path]::GetFullPath((Join-Path $BuildRoot $_)) }
foreach ($source in $sources) {
    if (-not [IO.File]::Exists($source)) { throw "Missing production JAR: $source" }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup = Join-Path (Join-Path $gameDirectory 'animania-backups') "$stamp-pre-deploy"
[IO.Directory]::CreateDirectory($backup) | Out-Null
foreach ($source in $sources) {
    $name = [IO.Path]::GetFileName($source)
    $target = Join-Path $mods $name
    if ([IO.File]::Exists($target)) { Copy-Item -LiteralPath $target -Destination $backup }
    Copy-Item -LiteralPath $source -Destination $target -Force
}

Write-Output "BACKUP=$backup"
$sources | ForEach-Object {
    $target = Join-Path $mods ([IO.Path]::GetFileName($_))
    $hash = Get-FileHash -LiteralPath $target -Algorithm SHA256
    Write-Output "$([IO.Path]::GetFileName($target))|$((Get-Item -LiteralPath $target).Length)|$($hash.Hash)"
}
