param(
  [string]$PrometheusHome = $env:PROMETHEUS_HOME,
  [string]$GrafanaHome = $env:GRAFANA_HOME,
  [switch]$NoMinimize
)

$ErrorActionPreference = 'Stop'

function Find-ExeInSubDirs {
  param(
    [Parameter(Mandatory = $true)][string]$Root,
    [Parameter(Mandatory = $true)][string]$ExeName,
    [int]$MaxDepth = 3
  )
  $Root = [string]$Root
  if ([string]::IsNullOrWhiteSpace($Root)) { return $null }
  if (!(Test-Path $Root)) { return $null }

  # Depth 0
  $p0 = Join-Path $Root $ExeName
  if (Test-Path $p0) { return $p0 }

  if ($MaxDepth -le 0) { return $null }

  # Depth 1..MaxDepth (manual BFS, compatible with Windows PowerShell 5.1)
  $queue = @(@{ dir = $Root; depth = 0 })
  while ($queue.Count -gt 0) {
    $head = $queue[0]
    if ($queue.Count -eq 1) { $queue = @() } else { $queue = $queue[1..($queue.Count - 1)] }

    $dir = [string]$head.dir
    $depth = [int]$head.depth
    if ($depth -ge $MaxDepth) { continue }

    $subDirs = @()
    try {
      $subDirs = Get-ChildItem -Path $dir -Directory -ErrorAction SilentlyContinue
    } catch {
      $subDirs = @()
    }

    foreach ($sd in $subDirs) {
      $p = Join-Path $sd.FullName $ExeName
      if (Test-Path $p) { return $p }
      $queue += @{ dir = $sd.FullName; depth = ($depth + 1) }
    }
  }

  return $null
}

function Resolve-ToolPath {
  param(
    [Parameter(Mandatory = $true)][string]$ExeName,
    [Parameter(Mandatory = $true)][string[]]$Candidates
  )
  # Flatten candidates (also supports env vars like "C:\a;D:\b")
  $flat = @()
  foreach ($c in $Candidates) {
    if ($null -eq $c) { continue }
    $s = [string]$c
    if ([string]::IsNullOrWhiteSpace($s)) { continue }
    foreach ($part in ($s -split ';')) {
      $p = $part.Trim()
      if ($p) { $flat += $p }
    }
  }

  foreach ($root in $flat) {
    $found = Find-ExeInSubDirs -Root $root -ExeName $ExeName -MaxDepth 3
    if ($found) { return $found }
  }
  return $null
}

$promConfig = Join-Path $PSScriptRoot 'prometheus.yml'
if (!(Test-Path $promConfig)) {
  throw ("prometheus.yml not found: " + $promConfig)
}

$promCandidates = @(
  $PrometheusHome,
  'D:\soft\prometheus',
  'D:\prometheus',
  'C:\soft\prometheus',
  'C:\prometheus'
)

$grafanaCandidates = @(
  (Join-Path $GrafanaHome 'bin'),
  $GrafanaHome,
  'D:\soft\grafana\bin',
  'D:\soft\grafana',
  'C:\soft\grafana\bin',
  'C:\soft\grafana',
  'C:\Program Files\GrafanaLabs\grafana\bin'
)

$promExe = Resolve-ToolPath -ExeName 'prometheus.exe' -Candidates $promCandidates
if (-not $promExe) {
  throw 'prometheus.exe not found. Set PROMETHEUS_HOME to the folder that contains prometheus.exe.'
}

$grafanaExe = Resolve-ToolPath -ExeName 'grafana-server.exe' -Candidates $grafanaCandidates
if (-not $grafanaExe) {
  throw 'grafana-server.exe not found. Set GRAFANA_HOME to the Grafana folder (or bin folder) that contains grafana-server.exe.'
}

Write-Host ('Prometheus: ' + $promExe)
Write-Host ('Grafana:    ' + $grafanaExe)
Write-Host ('Config:     ' + $promConfig)

$logsDir = Join-Path $PSScriptRoot 'logs'
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
$promLog = Join-Path $logsDir 'prometheus.log'
$grafanaLog = Join-Path $logsDir 'grafana.log'

$ws = if ($NoMinimize) { 'Normal' } else { 'Minimized' }

$promProc = Start-Process -FilePath $promExe `
  -WorkingDirectory (Split-Path -Parent $promExe) `
  -ArgumentList @("--config.file=$promConfig") `
  -RedirectStandardOutput $promLog `
  -RedirectStandardError $promLog `
  -WindowStyle $ws `
  -PassThru

Write-Host ('Prometheus PID: ' + $promProc.Id)

Start-Sleep -Seconds 1

$grafProc = Start-Process -FilePath $grafanaExe `
  -WorkingDirectory (Split-Path -Parent $grafanaExe) `
  -RedirectStandardOutput $grafanaLog `
  -RedirectStandardError $grafanaLog `
  -WindowStyle $ws `
  -PassThru

Write-Host ('Grafana PID:    ' + $grafProc.Id)

function Wait-Port {
  param([int]$Port, [int]$TimeoutSeconds = 10)
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $ok = Test-NetConnection -ComputerName 'localhost' -Port $Port -WarningAction SilentlyContinue
      if ($ok.TcpTestSucceeded) { return $true }
    } catch { }
    Start-Sleep -Milliseconds 500
  }
  return $false
}

$promUp = Wait-Port -Port 9090 -TimeoutSeconds 10
$grafUp = Wait-Port -Port 3000 -TimeoutSeconds 15

Write-Host ''
Write-Host 'Started:'
Write-Host (' - Prometheus: http://localhost:9090/targets' + ($(if ($promUp) { ' (OK)' } else { ' (NOT LISTENING)' })))
Write-Host (' - Grafana:    http://localhost:3000 (default admin/admin)' + ($(if ($grafUp) { ' (OK)' } else { ' (NOT LISTENING YET)' })))
Write-Host ''
Write-Host 'Logs:'
Write-Host (' - ' + $promLog)
Write-Host (' - ' + $grafanaLog)

if (-not $promUp) {
  Write-Host ''
  Write-Host 'Prometheus did not start listening on :9090. Check prometheus.log for details:'
  Write-Host ('  ' + $promLog)
}

