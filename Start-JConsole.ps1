#!/usr/bin/env pwsh

# Runs the JavaFX app from source; -Swing runs the 1.1 Swing UI instead, kept as the reference until 1.2.0.
# The first other arg (if any) is the connection target (host:port or a service:jmx: URL).
param(
    [switch]$Swing,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Target
)

# The build runs on a JDK 27 that bundles JavaFX (the enforcer refuses another). When Use-Jdk27.ps1, the
# session-scoped JDK switcher from common-scripts, is on PATH, the script switches this session to it first.
$useJdk = Get-Command 'Use-Jdk27.ps1' -ErrorAction SilentlyContinue
if ($useJdk) {
    . $useJdk.Path
}

$module = if ($Swing) { 'jconsole-booster-swing' } else { 'jconsole-booster-fx/jconsole-booster' }
$pom = Join-Path $PSScriptRoot 'pom.xml'

if ($Target.Count -gt 0) {
    mvn -f $pom -pl $module -am compile exec:exec@start "-Djconsole.target=$($Target[0])"
} else {
    mvn -f $pom -pl $module -am compile exec:exec@start
}
