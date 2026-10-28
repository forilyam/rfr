import assert from "node:assert/strict";
import {spawnSync} from "node:child_process";
import {fileURLToPath} from "node:url";
import test from "node:test";

function decision(command) {
    const result = spawnSync("codex", ["execpolicy", "check", "--rules", process.argv[2] ?? fileURLToPath(new URL("./project.rules", import.meta.url)), "--", ...command], {encoding: "utf8", timeout: 10000});
    if (result.error || result.status !== 0) {
        throw new Error(`Policy evaluation failed for ${JSON.stringify(command)}: ${result.error?.message ?? result.stderr}`);
    }
    return JSON.parse(result.stdout).decision;
}

for (const prefix of [[], ["rtk"], ["rtk", "proxy"], ["rtk", "bypass"], ["/opt/homebrew/bin/rtk", "proxy"]]) {
    for (const command of [
        ["git", "push"],
        ["git", "-C", ".", "push"],
        ["git", "-C", "/private/tmp/another-repository", "push"],
        ["git", "-C.", "push"],
        ["git", "-c", "alias.publish=push", "publish"],
        ["git", "--git-dir", ".git", "push"],
        ["git", "--git-dir=.git", "push"],
        ["git", "--no-pager", "push"],
        ["git", "send-pack", "/private/tmp/missing-remote", "HEAD"],
        ["git", "rm", "example.txt"],
        ["git", "clean", "-fd"],
        ["/usr/bin/git", "-C", ".", "push"],
        ["/opt/homebrew/bin/git", "rm", "example.txt"],
        ["rm", "example.txt"],
        ["grm", "example.txt"],
        ["/bin/rm", "example.txt"],
        ["unlink", "example.txt"],
        ["/usr/bin/unlink", "example.txt"],
        ["/opt/homebrew/bin/gunlink", "example.txt"],
        ["rmdir", "example-directory"],
        ["find", ".", "-name", "example.txt", "-delete"],
        ["gfind", ".", "-exec", "rm", "{}", ";"],
        ["/usr/bin/find", ".", "-delete"],
        ["/opt/homebrew/opt/coreutils/libexec/gnubin/rm", "example.txt"],
        ["env", "git", "push"],
        ["/usr/bin/env", "git", "-C", ".", "push"],
        ["env", "-i", "unlink", "example.txt"],
        ["xargs", "unlink"],
        ["command", "git", "push"],
        ["exec", "unlink", "example.txt"],
    ]) {
        test(`${[...prefix, ...command].join(" ")} cannot bypass forbidden rules`, () => {
            assert.equal(decision([...prefix, ...command]), "forbidden", "The bypass command was not denied");
        });
    }
}

for (const command of [
    ["git", "-C", ".", "status"],
    ["find", ".", "-type", "f"],
    ["env", "mv", "example.txt", ".example.txt.1793200000.bak"],
]) {
    test(`${command.join(" ")} cannot bypass the whole prefix restriction`, () => {
        assert.equal(decision(command), "forbidden", "The approved whole prefix restriction was not enforced");
    });
}

for (const command of [
    ["git", "status", "--short"],
    ["git", "diff"],
    ["git", "add", "--", "example.txt"],
    ["git", "commit", "-m", "Example"],
    ["rg", "example", "."],
    ["fd", "example"],
    ["mv", "example.txt", ".example.txt.1793200000.bak"],
    ["/bin/mv", "example.txt", ".example.txt.1793200000.bak"],
    ["rtk", "proxy", "mv", "example.txt", ".example.txt.1793200000.bak"],
]) {
    test(`${command.join(" ")} cannot be denied by the new restrictions`, () => {
        assert.notEqual(decision(command), "forbidden", "A normal command was denied");
    });
}

for (const command of [
    ["./gradlew", "e2e"],
    ["./gradlew", "runAll"],
    ["npm", "run", "dev"],
    ["docker", "compose", "up", "-d", "mysql"],
    ["kill", "-TERM", "12345"],
]) {
    test(`${command.join(" ")} cannot lose its existing permission`, () => {
        assert.equal(decision(command), "allow", "An existing permission was lost");
    });
}
