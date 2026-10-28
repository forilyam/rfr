#!/usr/bin/env bash
set -euo pipefail

checkout() {
    local directory tree commit
    directory=$(mktemp -d "${TMPDIR:-/tmp}/rfr-bash-hook-test.XXXXXX")
    git init --bare "$directory/repository.git" > "$directory/git.log" 2>&1
    tree=$(git --git-dir="$directory/repository.git" mktree < /dev/null)
    commit=$(printf 'Hook fixture\n' | git -c user.name='Hook test' -c user.email=hook@example.invalid --git-dir="$directory/repository.git" commit-tree "$tree")
    git --git-dir="$directory/repository.git" worktree add --detach "$directory/checkout" "$commit" >> "$directory/git.log" 2>&1
    mkdir -p "$directory/checkout/.codex/hooks" "$directory/bin" "$directory/tmp"
    cp "$1" "$directory/checkout/.codex/hooks/compile_changed.sh"
    cat > "$directory/checkout/gradlew" <<'COMPILER'
#!/usr/bin/env bash
printf 'gradle %s\n' "$*" >> "$RFR_HOOK_CALLS"
printf 'irregular compiler diagnostic\n' >&2
if [[ "$RFR_FAKE_DELAY" != 0 ]]; then sleep "$RFR_FAKE_DELAY"; fi
exit "$RFR_FAKE_STATUS"
COMPILER
    cat > "$directory/bin/npm" <<'COMPILER'
#!/usr/bin/env bash
printf 'npm %s\n' "$*" >> "$RFR_HOOK_CALLS"
printf 'irregular %s diagnostic\n' "$2" >&2
exit "$RFR_FAKE_STATUS"
COMPILER
    chmod +x "$directory/checkout/gradlew" "$directory/bin/npm"
    printf '%s\n' "$directory"
}

invoke() {
    printf '{"session_id":"bash-hook-verification"}\n' | env PATH="$1/bin:$PATH" TMPDIR="$1/tmp" RFR_HOOK_CALLS="$1/calls" RFR_FAKE_STATUS="$3" RFR_FAKE_DELAY="$4" RFR_HOOK_TIMEOUT="$5" bash "$1/checkout/.codex/hooks/compile_changed.sh" > "$2"
}

context() {
    node -e 'const fs = require("node:fs"); const response = JSON.parse(fs.readFileSync(process.argv[1], "utf8")); process.stdout.write(response.hookSpecificOutput?.additionalContext || "");' "$1"
}

match() {
    if ! printf '%s\n' "$1" | rg -Uq -- "$2"; then
        printf 'FAIL: %s\n%s\n' "$3" "$1" >&2
        return 1
    fi
}

equal() {
    if [[ "$1" != "$2" ]]; then
        printf 'FAIL: %s; received <%s>, expected <%s>\n' "$3" "$1" "$2" >&2
        return 1
    fi
}

test_java_errors_cannot_disappear_from_context() {
    local directory
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-auth/src/main/java"
    printf 'broken java\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    invoke "$directory" "$directory/response.json" 17 0 7
    match "$(context "$directory/response.json")" 'Exit code: 17\nirregular compiler diagnostic' 'Java failure was lost from hook context'
}

test_test_sources_cannot_execute_tests() {
    local directory
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-e2e/src/test/java"
    printf 'class ExampleTest {}\n' > "$directory/checkout/rfr-e2e/src/test/java/ExampleTest.java"
    invoke "$directory" "$directory/response.json" 0 0 7
    equal "$(cat "$directory/calls")" 'gradle :rfr-e2e:compileTestJava --console=plain' 'Test sources were not compiled without execution'
}

test_frontend_failure_cannot_skip_lint() {
    local directory
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-gql-client/src"
    printf 'broken typescript\n' > "$directory/checkout/rfr-gql-client/src/example.tsx"
    invoke "$directory" "$directory/response.json" 23 0 7
    match "$(context "$directory/response.json")" '(?s)Exit code: 23.*irregular build diagnostic.*Exit code: 23.*irregular lint diagnostic' 'Frontend failure lost build or lint feedback'
}

test_unchanged_failures_dont_repeat_compilation() {
    local directory count
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-api/src/main/java"
    printf 'broken java\n' > "$directory/checkout/rfr-api/src/main/java/Example.java"
    invoke "$directory" "$directory/first.json" 31 0 7
    invoke "$directory" "$directory/second.json" 31 0 7
    count=$(wc -l < "$directory/calls")
    equal "$((count))" 1 'An unchanged failure caused repeated compilation'
}

test_new_edits_cannot_reuse_stale_compilation() {
    local directory count
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-auth/src/main/java"
    printf 'class Example {}\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    invoke "$directory" "$directory/first.json" 0 0 7
    printf 'class Example { int irregular = 37; }\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    invoke "$directory" "$directory/second.json" 0 0 7
    count=$(wc -l < "$directory/calls")
    equal "$((count))" 2 'A later source edit reused stale compilation'
}

test_backup_moves_cannot_skip_compilation() {
    local directory count
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-api/src/main/java"
    printf 'class Example {}\n' > "$directory/checkout/rfr-api/src/main/java/Example.java"
    invoke "$directory" "$directory/first.json" 0 0 7
    mv "$directory/checkout/rfr-api/src/main/java/Example.java" "$directory/checkout/rfr-api/src/main/java/.Example.java.47.bak"
    invoke "$directory" "$directory/second.json" 0 0 7
    count=$(wc -l < "$directory/calls")
    equal "$((count))" 2 'A Java source moved to backup was missed'
}

test_documentation_changes_dont_start_compilation() {
    local directory
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-gql-client"
    printf 'Documentation only\n' > "$directory/checkout/rfr-gql-client/README.md"
    invoke "$directory" "$directory/response.json" 0 0 7
    equal "$(context "$directory/response.json")" '' 'Documentation caused unrelated compilation'
}

test_concurrent_calls_cannot_compile_the_same_snapshot_twice() {
    local directory first second count
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-auth/src/main/java"
    printf 'class Example {}\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    invoke "$directory" "$directory/first.json" 0 0.2 7 &
    first=$!
    invoke "$directory" "$directory/second.json" 0 0.2 7 &
    second=$!
    wait "$first"
    wait "$second"
    count=$(wc -l < "$directory/calls")
    equal "$((count))" 1 'Concurrent hook calls compiled the same source twice'
}

test_staging_files_dont_start_another_compilation() {
    local directory count
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-auth/src/main/java"
    printf 'class Example {}\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    invoke "$directory" "$directory/first.json" 0 0 7
    git -C "$directory/checkout" add -- rfr-auth/src/main/java/Example.java
    invoke "$directory" "$directory/second.json" 0 0 7
    count=$(wc -l < "$directory/calls")
    equal "$((count))" 1 'Staging unchanged files caused repeated compilation'
}

test_timeouts_cannot_lose_partial_output() {
    local directory
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-auth/src/main/java"
    printf 'class Example {}\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    invoke "$directory" "$directory/response.json" 0 11 2
    match "$(context "$directory/response.json")" '(?s)Timed out.*irregular compiler diagnostic' 'Compiler timeout lost its partial diagnostic'
}

test_missing_compilers_cannot_hide_their_errors() {
    local directory
    directory=$(checkout "$1")
    mkdir -p "$directory/checkout/rfr-auth/src/main/java"
    printf 'class Example {}\n' > "$directory/checkout/rfr-auth/src/main/java/Example.java"
    mv "$directory/checkout/gradlew" "$directory/checkout/.gradlew.53.bak"
    invoke "$directory" "$directory/response.json" 0 0 7
    match "$(context "$directory/response.json")" '(?s)Exit code: [1-9][0-9]*.*gradlew' 'An unavailable compiler did not return actionable feedback'
}

hook=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)/compile_changed.sh
if [[ ! -f "$hook" ]]; then
    printf 'FAIL: Bash compilation hook is missing\n' >&2
    exit 1
fi
for test in test_java_errors_cannot_disappear_from_context test_test_sources_cannot_execute_tests test_frontend_failure_cannot_skip_lint test_unchanged_failures_dont_repeat_compilation test_new_edits_cannot_reuse_stale_compilation test_backup_moves_cannot_skip_compilation test_documentation_changes_dont_start_compilation test_concurrent_calls_cannot_compile_the_same_snapshot_twice test_staging_files_dont_start_another_compilation test_timeouts_cannot_lose_partial_output test_missing_compilers_cannot_hide_their_errors; do
    "$test" "$hook"
    printf 'PASS: %s\n' "$test"
done
