$ErrorActionPreference = "Stop"

$ContainerName = "redis-dev"
$ImageName = "redis:7"
$StartPort = 10000
$EndPort = 65000

function Get-ExcludedPortRanges {
    $ranges = @()

    $output = netsh interface ipv4 show excludedportrange protocol=tcp

    foreach ($line in $output) {
        if ($line -match "^\s*(\d+)\s+(\d+)") {
            $ranges += [PSCustomObject]@{
                Start = [int]$matches[1]
                End   = [int]$matches[2]
            }
        }
    }

    return $ranges
}

function Get-ListeningPorts {
    try {
        return @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty LocalPort |
            Sort-Object -Unique)
    }
    catch {
        return @()
    }
}

function Test-PortUnavailable {
    param (
        [int]$Port,
        [array]$ExcludedRanges,
        [array]$ListeningPorts
    )

    if ($ListeningPorts -contains $Port) {
        return $true
    }

    foreach ($range in $ExcludedRanges) {
        if ($Port -ge $range.Start -and $Port -le $range.End) {
            return $true
        }
    }

    return $false
}

function Remove-OldRedisContainer {
    $existing = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $ContainerName }

    if ($existing) {
        Write-Host "Removing old container: $ContainerName"
        docker rm -f $ContainerName | Out-Null
    }
}

Write-Host "Checking Docker..."
docker --version

Write-Host "Pulling Redis image..."
docker pull $ImageName

Write-Host "Reading Windows excluded TCP port ranges..."
$excludedRanges = Get-ExcludedPortRanges

Write-Host "Reading current listening ports..."
$listeningPorts = Get-ListeningPorts

$selectedPort = $null

for ($port = $StartPort; $port -le $EndPort; $port++) {
    if (Test-PortUnavailable -Port $port -ExcludedRanges $excludedRanges -ListeningPorts $listeningPorts) {
        continue
    }

    Write-Host "Trying port $port ..."

    Remove-OldRedisContainer

    $dockerOutput = docker run -d --name $ContainerName -p "127.0.0.1:${port}:6379" $ImageName 2>&1

    if ($LASTEXITCODE -eq 0) {
        $selectedPort = $port
        Write-Host "Redis container started on localhost:$selectedPort"
        break
    }
    else {
        Write-Warning "Port $port failed:"
        Write-Warning $dockerOutput
        docker rm -f $ContainerName 2>$null | Out-Null
    }
}

if (-not $selectedPort) {
    throw "No available port found between $StartPort and $EndPort."
}

Start-Sleep -Seconds 2

Write-Host "Testing Redis..."
$ping = docker exec $ContainerName redis-cli ping

if ($ping.Trim() -ne "PONG") {
    docker logs $ContainerName
    throw "Redis started, but ping failed."
}

Write-Host ""
Write-Host "Redis is ready."
Write-Host "Host: localhost"
Write-Host "Port: $selectedPort"
Write-Host ""
Write-Host "Run these before starting Spring Boot:"
Write-Host "`$env:REDIS_HOST=`"localhost`""
Write-Host "`$env:REDIS_PORT=`"$selectedPort`""
Write-Host "`$env:REDIS_PASSWORD=`"`""
Write-Host "`$env:REDIS_DATABASE=`"0`""
Write-Host ""
Write-Host "Then start backend:"
Write-Host "cd E:\Bawa_Data\Xiangmu\ai-code-helper-my"
Write-Host "mvn spring-boot:run"