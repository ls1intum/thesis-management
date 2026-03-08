#!/bin/bash

set -euo pipefail

# Configuration
BACKUP_DIR="./backups"
DB_CONTAINER="thesis-management-db"
UPLOADS_DIR="./thesis_uploads"
LOG_FILE="$BACKUP_DIR/backup.log"
DATE=$(date +"%Y%m%d_%H%M%S")
DAY_OF_WEEK=$(date +%u)

# Retention
DB_RETENTION_DAYS=50
RSYNC_RETENTION_DAYS=50
MAX_FULL_ZIPS=3

# Extract DB credentials from .env.prod
DB_USER=$(grep '^SPRING_DATASOURCE_USERNAME=' .env.prod | cut -d '=' -f 2)
DB_NAME=$(grep '^SPRING_DATASOURCE_DATABASE=' .env.prod | cut -d '=' -f 2)
DB_PASSWORD=$(grep '^SPRING_DATASOURCE_PASSWORD=' .env.prod | cut -d '=' -f 2)

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

# Log all output
exec > >(tee -a "$LOG_FILE") 2>&1
echo "=========================================="
echo "Backup started at $(date)"

# Remove old DB backups
find "$BACKUP_DIR" -maxdepth 1 -type f -mtime +$DB_RETENTION_DAYS -name "db_backup_*.sql.gz" -exec rm -f {} \;

# Remove old rsync snapshots
find "$BACKUP_DIR" -maxdepth 1 -type d -mtime +$RSYNC_RETENTION_DAYS -name "files_*" -exec rm -rf {} \;

# Keep only the newest MAX_FULL_ZIPS full zip backups
FULL_ZIPS=$(find "$BACKUP_DIR" -maxdepth 1 -type f -name "backup_*.zip" | sort -r)
ZIP_COUNT=0
for zip in $FULL_ZIPS; do
  ZIP_COUNT=$((ZIP_COUNT + 1))
  if [ "$ZIP_COUNT" -gt "$MAX_FULL_ZIPS" ]; then
    echo "Removing old full backup: $(basename "$zip")"
    rm -f "$zip"
  fi
done

# Dump PostgreSQL database (compressed)
echo "Backing up PostgreSQL database..."
docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_DIR/db_backup_$DATE.sql.gz"

# Verify dump is not empty
if [ ! -s "$BACKUP_DIR/db_backup_$DATE.sql.gz" ]; then
  echo "Error: Database dump is empty!"
  rm -f "$BACKUP_DIR/db_backup_$DATE.sql.gz"
  exit 1
fi

echo "Database backup: db_backup_$DATE.sql.gz ($(du -h "$BACKUP_DIR/db_backup_$DATE.sql.gz" | cut -f1))"

# Daily: rsync snapshot with hardlinks to previous backup (only changed files use new space)
echo "Creating rsync snapshot of $UPLOADS_DIR..."
LATEST_LINK="$BACKUP_DIR/files_latest"
SNAPSHOT_DIR="$BACKUP_DIR/files_$DATE"
ABSOLUTE_LATEST="$(cd "$BACKUP_DIR" && pwd)/files_latest"

if [ -d "$LATEST_LINK" ]; then
  rsync -a --link-dest="$ABSOLUTE_LATEST" "$UPLOADS_DIR/" "$SNAPSHOT_DIR/"
else
  rsync -a "$UPLOADS_DIR/" "$SNAPSHOT_DIR/"
fi

ln -snf "files_$DATE" "$LATEST_LINK"
echo "Rsync snapshot: files_$DATE ($(du -sh "$SNAPSHOT_DIR" | cut -f1))"

# Weekly (Sunday): create a full zip backup, keep at most MAX_FULL_ZIPS
if [ "$DAY_OF_WEEK" -eq 7 ]; then
  echo "Weekly full backup: creating backup_$DATE.zip..."
  zip -rq "$BACKUP_DIR/backup_$DATE.zip" "$UPLOADS_DIR"
  echo "Full zip backup: backup_$DATE.zip ($(du -h "$BACKUP_DIR/backup_$DATE.zip" | cut -f1))"
fi

echo "Backup completed successfully."
echo "=========================================="

# Restore instructions:
#
# Restore database:
#   gunzip -c backups/db_backup_YYYYMMDD_HHMMSS.sql.gz | docker exec -i thesis-management-db psql -U <DB_USER> <DB_NAME>
#   (To restore into a fresh database, first drop and recreate it:
#     docker exec thesis-management-db dropdb -U <DB_USER> <DB_NAME>
#     docker exec thesis-management-db createdb -U <DB_USER> <DB_NAME>
#   )
#
# Restore files from rsync snapshot (any snapshot is a complete copy):
#   rsync -a --delete backups/files_YYYYMMDD_HHMMSS/ ./thesis_uploads/
#
# Restore files from weekly zip:
#   rm -rf ./thesis_uploads
#   unzip backups/backup_YYYYMMDD_HHMMSS.zip
