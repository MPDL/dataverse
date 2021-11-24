#!/bin/bash

####
# Configure edmond-specific settings in dataverse #
####



echo "Enable edmond@mpdl.mpg.de as system email"
curl -X PUT -d 'edmond@mpdl.mpg.de' http://localhost:8080/api/admin/settings/:SystemEmail

echo "Enable tabular ingest processing for migration"
curl -X PUT -d 2000000000 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit

echo "Enable checksum validation on publish for files > 2GB!!"
curl -X PUT -d 2000000000 http://localhost:8080/api/admin/settings/:DataFileChecksumValidationSizeLimit

