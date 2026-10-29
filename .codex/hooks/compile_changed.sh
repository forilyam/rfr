#!/usr/bin/env bash
set -uo pipefail

category() {
    case "$1" in
        rfr-gql-client/*/node_modules/*|rfr-gql-client/node_modules/*|rfr-gql-client/*/dist/*|rfr-gql-client/dist/*|rfr-gql-client/*/build/*|rfr-gql-client/build/*) ;;
        rfr-gql-client/*)
            case "$(printf '%s' "${1##*.}" | tr '[:upper:]' '[:lower:]')" in
                ts|tsx|js|jsx|mjs|cjs|json|css|scss|sass|less|html|svg) printf 'frontend' ;;
            esac
            ;;
        rfr-auth/src/main/*.java) printf 'rfr-auth-main' ;;
        rfr-auth/src/test/*.java) printf 'rfr-auth-test' ;;
        rfr-api/src/main/*.java) printf 'rfr-api-main' ;;
        rfr-api/src/test/*.java) printf 'rfr-api-test' ;;
        rfr-e2e/src/main/*.java) printf 'rfr-e2e-main' ;;
        rfr-e2e/src/test/*.java) printf 'rfr-e2e-test' ;;
    esac
}

fingerprint() {
    local root=$1 manifest=$2 path
    shift 2
    while IFS= read -r -d '' path; do
        set -- "$@" "$root/$path"
    done < "$manifest"
    if [[ $# -gt 0 ]]; then
        shasum -a 256 -- "$@" | shasum -a 256
    else
        printf '' | shasum -a 256
    fi
}

execute() {
    local directory=$1 prefix=$2 pid watcher status attempt
    shift 2
    printf 'Directory: %s\n$' "$directory"
    printf ' %q' "$@"
    printf '\n'
    (cd "$directory" && exec "$@") 9>&- > "$prefix.output" 2>&1 &
    pid=$!
    (
        sleep "$RFR_HOOK_TIMEOUT"
        : > "$prefix.timeout"
        if kill -TERM -- "-$pid" 2>/dev/null; then
            for ((attempt = 0; attempt < 50; attempt++)); do
                if ! kill -0 -- "-$pid" 2>/dev/null; then exit 0; fi
                sleep 0.1
            done
            kill -KILL -- "-$pid" 2>/dev/null || :
        fi
    ) 9>&- > "$prefix.watchdog" 2>&1 &
    watcher=$!
    if wait "$pid" 2>/dev/null; then status=0; else status=$?; fi
    if [[ -f "$prefix.timeout" ]]; then
        wait "$watcher" 2>/dev/null || :
        printf 'Timed out after %s seconds\n' "$RFR_HOOK_TIMEOUT"
    else
        kill -TERM -- "-$watcher" 2>/dev/null || :
        wait "$watcher" 2>/dev/null || :
        printf 'Exit code: %s\n' "$status"
    fi
    cat "$prefix.output"
    printf '\n'
}

lint() {
    local root=$1 directory=$2 path project=false
    local files=()
    while IFS= read -r -d '' path; do
        case "$path" in
            rfr-gql-client/eslint.config.*|rfr-gql-client/package.json|rfr-gql-client/package-lock.json) project=true ;;
            rfr-gql-client/*.ts|rfr-gql-client/*.tsx|rfr-gql-client/*.js|rfr-gql-client/*.mjs|rfr-gql-client/*.cjs)
                if [[ -f "$root/$path" && ! -L "$root/$path" ]]; then files+=("${path#rfr-gql-client/}"); fi
                ;;
        esac
    done < "$directory/dirty"
    if [[ "$project" == true ]]; then
        execute "$root/rfr-gql-client" "$directory/lint" npm run lint
    elif [[ ${#files[@]} -gt 0 ]]; then
        execute "$root/rfr-gql-client" "$directory/lint" ./node_modules/.bin/eslint --report-unused-disable-directives --max-warnings 0 -- "${files[@]}"
    fi
}

review() {
    local root=$1 cache=$2 directory=$3 path group frontend=false
    local groups=(rfr-auth-main rfr-auth-test rfr-api-main rfr-api-test rfr-e2e-main rfr-e2e-test frontend)
    local tasks=()
    mkdir -p "$cache"
    exec 9> "$cache/lock"
    if command -v lockf > /dev/null 2>&1; then
        lockf -k -t 30 9
    elif command -v flock > /dev/null 2>&1; then
        flock -w 30 9
    else
        printf 'Compilation hook requires lockf or flock\n' >&2
        return 1
    fi
    for group in "${groups[@]}"; do : > "$directory/$group.paths"; done
    git -C "$root" ls-files --cached --others --exclude-standard -z | sort -zu > "$directory/paths"
    while IFS= read -r -d '' path; do
        group=$(category "$path")
        if [[ -n "$group" && -f "$root/$path" && ! -L "$root/$path" ]]; then
            printf '%s\0' "$path" >> "$directory/$group.paths"
        fi
    done < "$directory/paths"
    git -C "$root" diff --name-only --no-renames -z HEAD -- > "$directory/dirty"
    git -C "$root" ls-files --others --exclude-standard -z >> "$directory/dirty"
    if [[ ! -f "$cache/initialized" ]]; then
        while IFS= read -r -d '' path; do
            group=$(category "$path")
            if [[ -n "$group" ]]; then : > "$directory/$group.dirty"; fi
        done < "$directory/dirty"
    fi
    for group in "${groups[@]}"; do
        fingerprint "$root" "$directory/$group.paths" > "$directory/$group.hash"
    done
    for group in "${groups[@]}"; do
        if { [[ -f "$cache/initialized" ]] && ! cmp -s "$cache/$group.hash" "$directory/$group.hash"; } || [[ -f "$directory/$group.dirty" ]]; then
            case "$group" in
                frontend) frontend=true ;;
                *-main) tasks+=(":${group%-main}:compileJava") ;;
                *-test) tasks+=(":${group%-test}:compileTestJava") ;;
            esac
        fi
    done
    # Failed checks must not repeat until their input changes.
    for group in "${groups[@]}"; do cp "$directory/$group.hash" "$cache/$group.hash"; done
    : > "$cache/initialized"
    if [[ ${#tasks[@]} -gt 0 ]]; then
        execute "$root" "$directory/java" ./gradlew "${tasks[@]}" --console=plain
    fi
    if [[ "$frontend" == true ]]; then
        execute "$root/rfr-gql-client" "$directory/build" npm run build
        lint "$root" "$directory"
    fi
}

if [[ "${1:-}" == --review ]]; then
    set -e
    set -m
    RFR_HOOK_TIMEOUT=${RFR_HOOK_TIMEOUT:-180}
    if [[ ! "$RFR_HOOK_TIMEOUT" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
        printf 'Invalid compilation timeout: %s\n' "$RFR_HOOK_TIMEOUT" >&2
        exit 1
    fi
    review "$2" "$3" "$4"
    exit 0
fi

if ! command -v node > /dev/null 2>&1; then
    printf '{"hookSpecificOutput":{"hookEventName":"PostToolUse","additionalContext":"Compilation hook requires Node.js for JSON input and output"}}\n'
    exit 0
fi
umask 077
directory=$(mktemp -d "${TMPDIR:-/tmp}/rfr-compilation-run.XXXXXX") || exit 1
if session=$(node -e 'const fs = require("node:fs"); const event = JSON.parse(fs.readFileSync(0, "utf8")); if (typeof event.session_id !== "string" || !event.session_id || /[\0\r\n]/.test(event.session_id)) throw new Error("Invalid hook session_id"); process.stdout.write(event.session_id);' 2> "$directory/context"); then
    root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)
    identity=$(printf '%s\0%s' "$root" "$session" | shasum -a 256)
    cache="${TMPDIR:-/tmp}/rfr-compilation-bash-$UID/${identity%% *}"
    if bash "${BASH_SOURCE[0]}" --review "$root" "$cache" "$directory" > "$directory/context" 2>&1; then
        :
    else
        printf '\nCompilation hook failed with exit code: %s\n' "$?" >> "$directory/context"
    fi
else
    printf '\nCompilation hook could not read its JSON input\n' >> "$directory/context"
fi
node -e 'const fs = require("node:fs"); const context = fs.readFileSync(process.argv[1], "utf8"); console.log(JSON.stringify(context ? {hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: context}} : {}));' "$directory/context"
