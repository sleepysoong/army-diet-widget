#!/bin/bash

# pre-commit 스크립트를 .git/hooks로 복사 및 실행 권한 부여
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

echo "✅  Pre-commit hook installed successfully!"
