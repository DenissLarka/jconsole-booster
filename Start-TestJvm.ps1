#!/usr/bin/env pwsh

param(
    [int]$Port = 7091
)

# The build runs on a JDK 27 that bundles JavaFX (the enforcer refuses another). When Use-Jdk27.ps1, the
# session-scoped JDK switcher from common-scripts, is on PATH, the script switches this session to it first.
$useJdk = Get-Command 'Use-Jdk27.ps1' -ErrorAction SilentlyContinue
if ($useJdk) {
    . $useJdk.Path
}

$module = 'jconsole-booster-sample-target'

Write-Host "Starting sample target JVM on port $Port..."
Write-Host "Connect with: ./Start-JConsole.ps1 localhost:$Port"
Write-Host ""

mvn -pl $module -am compile "exec:exec@start-target" "-Dtarget.port=$Port"
