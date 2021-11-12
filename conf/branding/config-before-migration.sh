#!/bin/bash

####
# Configure edmond-specific settings in dataverse #
####

### SOLR index for dataverse ###
#SOLR_PATH=/srv/web/solr/solr-8.8.1
#SOLR_USER=solr

SOLR_PATH=/Users/haarlae1/Servers/solr-8.8.2
SOLR_USER=haarlae1

echo "Copying schema.xml and solrconfig.xml to collection1 index core"
cp ../solr/8.8.1/schema.xml $SOLR_PATH/server/solr/collection1/conf
cp ../solr/8.8.1/solrconfig.xml $SOLR_PATH/server/solr/collection1/conf

echo "Reload dataverse collection1 core"
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"

### SOLR index for ROR autosuggest ###
ROR_DATA_NAME=2021-09-23-ror-data
echo "Create solr core for ROR data"
sudo -u $SOLR_USER $SOLR_PATH/bin/solr create -c rordata

cp conf/solr/8.8.1-rorData/managed-schema $SOLR_PATH/server/solr/rordata/conf

echo "Reload rordata index schema"
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=rordata"

echo "Downloading ROR json file"
wget https://zenodo.org/record/5534443/files/$ROR_DATA_NAME.zip

unzip $ROR_DATA_NAME.zip

echo "Indexing ROR data"
curl -X POST -H "Content-type:application/json" -T $ROR_DATA_NAME.json 'http://localhost:8983/solr/rordata/update/json/docs?split=/&f=$FQN:/**&commit=true'


### DOI Settings ###
DOI_PREFIX=10.15771
DOI_SHOULDER='3./'

# Set DOI prefix
echo "Setting DOI prefix to $DOI_PREFIX"
curl -X PUT -d $DOI_PREFIX http://localhost:8080/api/admin/settings/:Authority

# Set a DOI Shoulder (TO BE DONE: WHICH SHOULDER???)
echo "Setting DOI shoulder to $DOI_SHOULDER"
curl -X PUT -d $DOI_SHOULDER http://localhost:8080/api/admin/settings/:Shoulder

# Disable File PIDs
echo "Disabling PIDs for files"
curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:FilePIDsEnabled



echo "Do not export contact emails as json"
curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport

echo "Disable email for migration- RESET after migration!!"
curl -X PUT -d '' http://localhost:8080/api/admin/settings/:SystemEmail

echo "Disable tabular ingest processing for migration -- RESET after migration!!"
curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit

echo "Disable checksum validation on publish for migration -- RESET after migration to 2GB!!"
curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:DataFileChecksumValidationSizeLimit

# Create MPG email domain group
### BEWARE: Change database table definition fpr persistedglobalgroup.emaildomains to text before
echo "Create MPG email domain group - BEWARE: Change database table definition for persistedglobalgroup.emaildomains to text before"
curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/domain --upload-file conf/mpg-domains.json

# Create MPG Shibboleth Group
echo "Create MPG Shibboleth group"
curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/shib --upload-file conf/mpg-shib-group.json


### Password Policy
# At least 8 characters
echo "Creating password policy..."
curl -X PUT -d 8 http://localhost:8080/api/admin/settings/:PVMinLength
# At least one upper case, one lower case, one digit, one special
echo
curl -X PUT -d 'UpperCase:1,LowerCase:1,Digit:1,Special:1' http://localhost:8080/api/admin/settings/:PVCharacterRules
echo
curl -X PUT -d 4 http://localhost:8080/api/admin/settings/:PVNumberOfCharacteristics
echo
curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:PVGoodStrength