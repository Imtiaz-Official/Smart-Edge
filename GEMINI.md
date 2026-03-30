# Workspace Instructions

- **Releases**: GitHub Actions for building and releasing APKs are triggered only by Git tags starting with 'v' (e.g., `git tag v1.2.1 && git push origin v1.2.1`).
- **PowerShell**: The local environment does not support the `&&` operator. Use `;` instead.
- **Backups**: Do not delete 'temp_databackup' or 'databackup' directories if present.