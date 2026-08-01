#!/usr/bin/env pwsh

param(
    [int]$Port = 7091
)

Write-Host "Starting sample target JVM on port $Port..."
Write-Host "Connect with: mvn exec:exec@start  (then enter localhost:$Port)"
Write-Host ""

mvn test-compile "exec:exec@start-target" "-Dtarget.port=$Port"
