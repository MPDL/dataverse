#!/bin/bash
#
# The purpose of this script is to deploy custom branding
#
############################################# !!!
#
# PLEASE, TAKE A LOOK AT VARIABLES BEFORE RUN !!!
#
############################################# !!!
projectHome=../..

destinationDocroot="/srv/web/payara5/glassfish/domains/domain1/docroot"
#destinationDocroot="/usr/local/payara5/glassfish/domains/domain1/docroot"

destinationCustomBrandingFiles="/srv/mpdl-dataverse/branding"

apiURL="http://localhost:8080/api/admin/settings/"

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
        printf "\n$usage\n"
        exit 1
      fi
      ;;
    *)
      printf "\n$usage\n"
      exit 1
      ;;
  esac
fi

log="/tmp/`basename $0`.`date +"%s"`.log";

printf "\n\nBranding Installation (process output to -> $log)\n" | tee -a $log

printf "\n\nPreparing destination folders:\n" | tee -a $log

if [ ! -d "$destinationDocroot/logos/navbar" ]; then
  mkdir -p $destinationDocroot/logos/navbar
  if [ $? -eq 0 ]; then
    printf "\n$destinationDocroot/logos/navbar created" | tee -a $log
    status="0"
  else
    printf "\n$destinationDocroot/logos/navbar could not be created!" | tee -a $log
    status="1"
  fi
else
  printf "\n$destinationDocroot/logos/navbar already exists" | tee -a $log
  status="0"
fi

if [ ! -d "$destinationDocroot/logos/images" ]; then
  mkdir -p $destinationDocroot/logos/images
  if [ $? -eq 0 ]; then
    printf "\n$destinationDocroot/logos/images created" | tee -a $log
    status+=0
  else
    printf "\n$destinationDocroot/logos/images could not be created!" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationDocroot/logos/images already exists" | tee -a $log
  status+=0
fi

if [ ! -d "$destinationDocroot/logos/fonts" ]; then
  mkdir -p $destinationDocroot/logos/fonts
  if [ $? -eq 0 ]; then
    printf "\n$destinationDocroot/logos/fonts created" | tee -a $log
    status+=0
  else
    printf "\n$destinationDocroot/logos/fonts could not be created!" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationDocroot/logos/fonts already exists" | tee -a $log
  status+=0
fi

if [ ! -d "$destinationCustomBrandingFiles" ]; then
  mkdir -p $destinationCustomBrandingFiles
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomBrandingFiles could not be created!" | tee -a $log
    status+=0
  else
    printf "\n$destinationCustomBrandingFiles created" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationCustomBrandingFiles already exists" | tee -a $log
  status+=0
fi

printf "\n\nCopying resources to destination:\n" | tee -a $log

cp -R $projectHome/conf/branding/resources/assets/* $destinationDocroot/logos/
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/assets/* copied to $destinationDocroot/logos/" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/assets/* to $destinationDocroot/logos/,this step failed!" | tee -a $log
  status+=1
fi

cp $projectHome/conf/branding/resources/css/*.css $destinationCustomBrandingFiles
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/css/*.css copied to $destinationCustomBrandingFiles" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/css/*.css to $destinationCustomBrandingFiles,this step failed!" | tee -a $log
  status+=1
fi

cp $projectHome/conf/branding/resources/mpdl-footer.html $destinationCustomBrandingFiles
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/assets/mpdl-footer.html copied to $destinationCustomBrandingFiles" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/assets/mpdl-footer.html to $destinationCustomBrandingFiles,this step failed!" | tee -a $log
  status+=1
fi

printf "\n\nSetting paths:\n\n" | tee -a $log

curl -X PUT -d "/logos/navbar/logo_for_bright.png" $apiURL:LogoCustomizationFile$unblock -q | tee -a $log
printf "\n" | tee -a $log
curl -X PUT -d "$destinationCustomBrandingFiles/mpdl-footer.html" $apiURL:FooterCustomizationFile$unblock -q | tee -a $log
printf "\n" | tee -a $log
curl -X PUT -d "$destinationCustomBrandingFiles/mpdl-stylesheet.css" $apiURL:StyleCustomizationFile$unblock -q | tee -a $log
printf "\n" | tee -a $log

if [ $status -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: $status... for more info see $log\n"
  exit 2
fi
