$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $raiz

if (Test-Path "out") {
    Remove-Item -Recurse -Force "out"
}
New-Item -ItemType Directory -Path "out" | Out-Null

$fuentes = Get-ChildItem -Recurse -Filter *.java -Path "src" | ForEach-Object { $_.FullName }
& javac --release 17 -encoding UTF-8 -d "out" $fuentes
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$argumentos = @(
    "-Dfile.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8",
    "-Dstderr.encoding=UTF-8",
    "-cp", "out",
    "com.tecnomovil.app.Main"
) + $args

$info = New-Object System.Diagnostics.ProcessStartInfo
$info.FileName = "java"
$info.Arguments = $argumentos -join " "
$info.WorkingDirectory = $raiz
$info.UseShellExecute = $false
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
$info.StandardOutputEncoding = [System.Text.UTF8Encoding]::new($false)
$info.StandardErrorEncoding = [System.Text.UTF8Encoding]::new($false)

$proc = [System.Diagnostics.Process]::Start($info)
$stdout = $proc.StandardOutput.ReadToEnd()
$stderr = $proc.StandardError.ReadToEnd()
$proc.WaitForExit()

[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Write-Output $stdout
if ($stderr) {
    [Console]::Error.WriteLine($stderr)
}

New-Item -ItemType Directory -Force -Path (Join-Path $raiz "evidencias") | Out-Null
$destino = Join-Path $raiz "evidencias\ejecucion.txt"
[System.IO.File]::WriteAllText($destino, $stdout, [System.Text.UTF8Encoding]::new($true))
exit $proc.ExitCode
