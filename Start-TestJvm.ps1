#!/usr/bin/env pwsh

param(
    [int]$Port = 7091
)

$module = 'jconsole-booster-sample-target'

Write-Host "Starting sample target JVM on port $Port..."
Write-Host "Connect with: ./Start-JConsole.ps1 localhost:$Port"
Write-Host ""

mvn -pl $module -am compile "exec:exec@start-target" "-Dtarget.port=$Port"
