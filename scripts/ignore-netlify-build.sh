#!/usr/bin/env bash

# Netlify build.ignore: 0 skips the build; 1 continues with the build.
if [[ -z "${CACHED_COMMIT_REF:-}" || -z "${COMMIT_REF:-}" ]]; then
  exit 1
fi

if [[ "$CACHED_COMMIT_REF" == "$COMMIT_REF" ]]; then
  exit 1
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)" || exit 1
repo_root="$(cd -- "$script_dir/.." && pwd)" || exit 1

if ! git -C "$repo_root" cat-file -e "${CACHED_COMMIT_REF}^{commit}" 2>/dev/null; then
  exit 1
fi

if ! git -C "$repo_root" cat-file -e "${COMMIT_REF}^{commit}" 2>/dev/null; then
  exit 1
fi

git -C "$repo_root" diff --quiet "$CACHED_COMMIT_REF" "$COMMIT_REF" -- \
  . \
  ':(exclude)backend/**' \
  ':(exclude)docs/**' \
  ':(exclude,icase,glob)**/README*'
diff_status=$?

if [[ "$diff_status" -eq 0 ]]; then
  exit 0
fi

# Any diff error is treated as a reason to build, never to skip a deploy.
exit 1