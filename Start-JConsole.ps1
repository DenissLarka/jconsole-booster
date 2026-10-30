#!/usr/bin/env pwsh

# Runs the UI from source. The first arg (if any) becomes the connection target,
# pre-filling the connect dialog (or auto-connecting on success).

# The build runs on a JDK 27 that bundles JavaFX (the enforcer refuses another). When Use-Jdk27.ps1, the
# session-scoped JDK switcher from common-scripts, is on PATH, the script switches this session to it first.
$useJdk = Get-Command 'Use-Jdk27.ps1' -ErrorAction SilentlyContinue
if ($useJdk) {
    . $useJdk.Path
}

$module = 'jconsole-booster-swing'

if ($args.Count -gt 0) {
    mvn -pl $module -am compile exec:exec@start "-Djconsole.target=$($args[0])"
} else {
    mvn -pl $module -am compile exec:exec@start
}
