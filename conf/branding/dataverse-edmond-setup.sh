#!/bin/bash

####
# Setup edmond-specific settings in dataverse #
####

### SOLR index for dataverse ###
SOLR_PATH=/srv/web/solr/solr-8.8.1
SOLR_USER=solr
ROR_DATA_NAME=v1.0-2022-03-17-ror-data

### SOLR index for ROR autosuggest ###
echo "Create solr core for ROR data"
sudo -u $SOLR_USER $SOLR_PATH/bin/solr create -c rordata

echo "Copy ROR managed-schema to solr"
cp conf/solr/8.8.1-rorData/managed-schema $SOLR_PATH/server/solr/rordata/conf

echo "Copying schema.xml, solrconfig.xml and filter_mapping.txt to collection1 index core"
cp ../solr/8.11.1/schema.xml $SOLR_PATH/server/solr/collection1/conf
cp ../solr/8.11.1/solrconfig.xml $SOLR_PATH/server/solr/collection1/conf
cp ../solr/8.11.1/filter_mapping.txt $SOLR_PATH/server/solr/collection1/conf

echo "Reload dataverse collection1 core"
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"

echo "Reload rordata index schema"
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=rordata"

echo "Downloading ROR json file"
wget https://zenodo.org/record/6347575/files/$ROR_DATA_NAME.json.zip

unzip $ROR_DATA_NAME.json.zip

echo "Indexing ROR data"
curl -X POST -H "Content-type:application/json" -T $ROR_DATA_NAME.json 'http://localhost:8983/solr/rordata/update/json/docs?split=/&f=$FQN:/**&commit=true'



### DOI Settings ###
DOI_PREFIX=10.15771
DOI_SHOULDER='3.'

# Set DOI prefix
echo "Setting DOI prefix to $DOI_PREFIX"
curl -X PUT -d $DOI_PREFIX http://localhost:8080/api/admin/settings/:Authority

# Set a DOI Shoulder (TO BE DONE: WHICH SHOULDER???)
echo "Setting DOI shoulder to $DOI_SHOULDER"
curl -X PUT -d $DOI_SHOULDER http://localhost:8080/api/admin/settings/:Shoulder

# Disable File PIDs
echo "Disabling PIDs for files"
curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:FilePIDsEnabled



# Further Steps

echo "Do not export contact emails as json"
curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport

echo "Enable edmond@mpdl.mpg.de as system email"
curl -X PUT -d 'edmond@mpdl.mpg.de' http://localhost:8080/api/admin/settings/:SystemEmail

echo "Enable tabular ingest processing"
curl -X PUT -d 2000000000 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit

echo "Disable zip file extraction on ingest"
curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:ZipUploadFilesLimit

echo "Enable checksum validation on publish for files > 2GB!!"
curl -X PUT -d 2000000000 http://localhost:8080/api/admin/settings/:DataFileChecksumValidationSizeLimit

echo "Enable Public Install (disable file-restrict functionality)"
curl -X PUT -d true http://localhost:8080/api/admin/settings/:PublicInstall

echo "Zip Download Limit"
curl -X PUT -d 5000000000 http://localhost:8080/api/admin/settings/:ZipDownloadLimit



# Create MPG email domain group
echo "Create MPG email domain group"
curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/domain --upload-file conf/mpg-domains.json

# Create MPG Shibboleth Group
echo "Create MPG Shibboleth group"
curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/shib --upload-file conf/mpg-shib-group.json



### Password Policy
# At least 10 characters, one upper case, one lower case, one digit, one special
echo "Creating password policy..."
echo "PW with at least 10 characters"
curl -X PUT -d 10 http://localhost:8080/api/admin/settings/:PVMinLength
echo "PW with at least one upper case & one lower case & one digit & one special character"
curl -X PUT -d 'UpperCase:1,LowerCase:1,Digit:1,Special:1' http://localhost:8080/api/admin/settings/:PVCharacterRules
echo "ALL defined password constraints must be fulfilled (independent of PW length)"
curl -X PUT -d 4 http://localhost:8080/api/admin/settings/:PVNumberOfCharacteristics
curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:PVGoodStrength