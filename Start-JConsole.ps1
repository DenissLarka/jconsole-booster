#!/usr/bin/env pwsh

# Pass-through wrapper. The first arg (if any) becomes the connection target,
# pre-filling the connect dialog (or auto-connecting on success).

if ($args.Count -gt 0) {
    mvn compile exec:exec@start "-Djconsole.target=$($args[0])"
} else {
    mvn compile exec:exec@start
}
