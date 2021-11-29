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
#destinationDocroot="/Users/haarlae1/Servers/payara5/glassfish/domains/domain1/docroot"

destinationCustomizationFiles="/srv/mpdl-dataverse-branding"
#destinationCustomizationFiles=/var/www/dataverse/branding
#destinationCustomizationFiles="/Users/haarlae1/Servers/mpdl-dataverse-branding"

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

if [ ! -d "$destinationCustomizationFiles" ]; then
  mkdir -p $destinationCustomizationFiles
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomizationFiles could not be created!" | tee -a $log
    status+=0
  else
    printf "\n$destinationCustomizationFiles created" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationCustomizationFiles already exists" | tee -a $log
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

cp $projectHome/conf/branding/resources/css/*.css $destinationCustomizationFiles
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/css/*.css copied to $destinationCustomizationFiles" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/css/*.css to $destinationCustomizationFiles,this step failed!" | tee -a $log
  status+=1
fi

cp $projectHome/conf/branding/resources/*.html $destinationCustomizationFiles
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/assets/*.html copied to $destinationCustomizationFiles" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/assets/*.html to $destinationCustomizationFiles,this step failed!" | tee -a $log
  status+=1
fi

printf "\n\nSetting paths:\n\n" | tee -a $log

curl -X PUT -d "/logos/navbar/logo_for_bright.png" http://localhost:8080/api/admin/settings/:LogoCustomizationFile$unblock -q | tee -a $log
printf "\n" | tee -a $log
curl -X PUT -d "$destinationCustomizationFiles/mpdl-footer.html" http://localhost:8080/api/admin/settings/:FooterCustomizationFile$unblock -q | tee -a $log
printf "\n" | tee -a $log
curl -X PUT -d "$destinationCustomizationFiles/mpdl-stylesheet.css" http://localhost:8080/api/admin/settings/:StyleCustomizationFile$unblock -q | tee -a $log
printf "\n" | tee -a $log

if [ $status -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: $status... for more info see $log\n"
  exit 2
fi