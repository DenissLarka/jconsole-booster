#!/usr/bin/env pwsh

# Runs the UI from source. The first arg (if any) becomes the connection target,
# pre-filling the connect dialog (or auto-connecting on success).

$module = 'jconsole-booster-swing'

if ($args.Count -gt 0) {
    mvn -pl $module -am compile exec:exec@start "-Djconsole.target=$($args[0])"
} else {
    mvn -pl $module -am compile exec:exec@start
}
