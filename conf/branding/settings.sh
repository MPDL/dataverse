#!/bin/bash
#
# The purpose of this script is to deploy custom branding
#
############################################# !!!
#
# PLEASE, TAKE A LOOK AT VARIABLES BEFORE RUN !!!
#
############################################# !!!
#projectHome=~/WorkSpace/MPDL/dataverse
projectHome=../..

destinationDocroot="/srv/web/payara5/glassfish/domains/domain1/docroot"
#destinationDocroot="/usr/local/payara5/glassfish/domains/domain1/docroot"

destinationCustomizationFiles="/srv/mpdl-dataverse-branding"
#destinationCustomizationFiles=/var/www/dataverse/branding

unblock="?unblock-key=blkAPI_dev_ed2" 
############################################## !!!
usage="usage: $0 [-k|--key <unblock-key>]"
if [[ $# -gt 0 ]]; then
  key="$1"
  case $key in
    -k|--key)
      if [[ $# -eq 2 ]]; then
        unblock="?unblock-key=$2"
      else
        echo $usage
        exit 1
      fi
      ;;
    *)
      echo $usage
      exit 1
      ;;      
  esac
fi

echo "Branding Installation ..."

if [ ! -d "$destinationDocroot/logos/navbar" ]; then
  mkdir -p $destinationDocroot/logos/navbar
  if [ $? -eq 0 ]; then
    echo "$destinationDocroot/logos/navbar created"
    status="0"
  else
    status="1"
  fi
else
  status="0"
fi

if [ ! -d "$destinationDocroot/logos/images" ]; then
  mkdir -p $destinationDocroot/logos/images
  if [ $? -eq 0 ]; then
    echo "$destinationDocroot/logos/images created"
    status+=0
  else
    status+=1
  fi
else
  status+=0
fi

if [ ! -d "$destinationCustomizationFiles" ]; then
  mkdir -p $destinationCustomizationFiles
  if [ $? -eq 0 ]; then
    echo "$destinationCustomizationFiles created"
    status+=0
  else
    status+=1
  fi
else
  status+=0
fi

echo "... copying resources to destination ..."

cp -R $projectHome/conf/branding/resources/assets/* $destinationDocroot/logos/
if [ $? -eq 0 ]; then
  echo "$projectHome/conf/branding/resources/assets/* copied to $destinationDocroot/logos/"
  status+=0
else
  status+=1
fi

cp $projectHome/conf/branding/resources/css/*.css $destinationCustomizationFiles
if [ $? -eq 0 ]; then
  echo "$projectHome/conf/branding/resources/css/*.css copied to $destinationCustomizationFiles"
  status+=0
else
  status+=1
fi

cp $projectHome/conf/branding/resources/*.html $destinationCustomizationFiles
if [ $? -eq 0 ]; then
  echo "$projectHome/conf/branding/resources/assets/*.html copied to $destinationCustomizationFiles"
  status+=0
else
  status+=1
fi

echo "... setting paths ..."
curl -X PUT -d "/logos/navbar/logo_for_bright.png" http://localhost:8080/api/admin/settings/:LogoCustomizationFile$unblock
echo
curl -X PUT -d "$destinationCustomizationFiles/mpdl-footer.html" http://localhost:8080/api/admin/settings/:FooterCustomizationFile$unblock
echo
curl -X PUT -d "$destinationCustomizationFiles/mpdl-stylesheet.css" http://localhost:8080/api/admin/settings/:StyleCustomizationFile$unblock
echo

if [ $status -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: $status...\n"
  exit 2
fi
