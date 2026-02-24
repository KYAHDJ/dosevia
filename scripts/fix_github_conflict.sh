#!/usr/bin/env bash
set -euo pipefail

# One-command sync helper for this repository.
# Rebases a branch on top of upstream main and force-pushes safely.

REMOTE="${1:-origin}"
BASE_BRANCH="${2:-main}"
TARGET_BRANCH="${3:-work}"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Error: run this script inside a git repository." >&2
  exit 1
fi

if ! git remote get-url "$REMOTE" >/dev/null 2>&1; then
  echo "Error: remote '$REMOTE' is not configured." >&2
  echo "Run: git remote add $REMOTE <repo-url>" >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Error: working tree is not clean. Commit or stash changes first." >&2
  exit 1
fi

echo "Fetching latest branches from $REMOTE..."
git fetch "$REMOTE" --prune

echo "Checking out $TARGET_BRANCH..."
git checkout "$TARGET_BRANCH"

echo "Rebasing $TARGET_BRANCH onto $REMOTE/$BASE_BRANCH..."
if ! git rebase "$REMOTE/$BASE_BRANCH"; then
  echo
  echo "Rebase stopped due to conflicts. Resolve them, then run:"
  echo "  git add <resolved-files>"
  echo "  git rebase --continue"
  echo
  echo "After rebase completes, push with:"
  echo "  git push $REMOTE $TARGET_BRANCH --force-with-lease"
  exit 1
fi

echo "Pushing $TARGET_BRANCH to $REMOTE (force-with-lease)..."
git push "$REMOTE" "$TARGET_BRANCH" --force-with-lease

echo "Done. '$TARGET_BRANCH' is now rebased on '$REMOTE/$BASE_BRANCH'."
