#!/usr/bin/env bash
docroot = '/usr/local/payara5/glassfish/domains/domain1/docroot'
customizationFiles = '/srv/mpdl-dataverse-branding'
echo "Branding Installation ..."
echo "... copy resources to destination ..."
if [ ! -d $docroot/logos/navbar ]; then
	mkdir -p $docroot/logos/navbar
fi
if [ ! -d $docroot/logos/images ]; then
	mkdir -p $docroot/logos/images
fi
cp -R resources/assets $docroot/logos
cp resources/css/*.css $customizationFiles
cp resources/*.html $customizationFiles
echo "... setting paths ..."
curl -X PUT -d "/logos/navbar/logo.png" http://localhost:8080/api/admin/settings/:LogoCustomizationFile
curl -X PUT -d "$customizationFiles/custom-footer.html" http://localhost:8080/api/admin/settings/:FooterCustomizationFile
curl -X PUT -d "$customizationFiles/custom-stylesheet.css" http://localhost:8080/api/admin/settings/:StyleCustomizationFile
echo $?
