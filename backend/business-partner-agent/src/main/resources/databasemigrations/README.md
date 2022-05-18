# Flyway Migration

## Rollup

Log in to container and run
```
pg_dump -U walletuser --schema-only --no-comments --no-owner --no-tablespaces
```
https://stackoverflow.com/questions/25506192/how-to-squash-merge-migrations-in-flyway
