#!/bin/bash
#
# The purpose of this script is to deploy custom legal documents for Dataverse
# issue-15 on github
#
############################################# !!!
#
# PLEASE, TAKE A LOOK AT VARIABLES BEFORE RUN !!!
#
############################################# !!!
#projectHome="../dataverse"
projectHome=../..

destinationCustomLegalFiles="/srv/mpdl-dataverse/guides/"
customLegalFilesURL="https://dev-edmond2.mpdl.mpg.de/guides" #Please, without last slash!!!
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

printf "\n\Custom Legal Documents Installation (process output to -> $log)\n" | tee -a $log

printf "\n\nPreparing destination folders:\n" | tee -a $log

if [ ! -d "$destinationCustomLegalFiles/logos/navbar" ]; then
  mkdir -p $destinationCustomLegalFiles/logos/navbar
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomLegalFiles/logos/navbar created" | tee -a $log
    status="0"
  else
    printf "\n$destinationCustomLegalFiles/logos/navbar could not be created!" | tee -a $log
    status="1"
  fi
else
  printf "\n$destinationCustomLegalFiles/logos/navbar already exists" | tee -a $log
  status="0"
fi

if [ ! -d "$destinationCustomLegalFiles/logos/images" ]; then
  mkdir -p $destinationCustomLegalFiles/logos/images
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomLegalFiles/logos/images created" | tee -a $log
    status+=0
  else
    printf "\n$destinationCustomLegalFiles/logos/images could not be created!" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationCustomLegalFiles/logos/images already exists" | tee -a $log
  status+=0
fi

if [ ! -d "$destinationCustomLegalFiles/logos/fonts" ]; then
  mkdir -p $destinationCustomLegalFiles/logos/fonts
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomLegalFiles/logos/fonts created" | tee -a $log
    status+=0
  else
    printf "\n$destinationCustomLegalFiles/logos/fonts could not be created!" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationCustomLegalFiles/logos/fonts already exists" | tee -a $log
  status+=0
fi

if [ ! -d "$destinationCustomLegalFiles/css" ]; then
  mkdir -p $destinationCustomLegalFiles/css
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomLegalFiles/css could not be created!" | tee -a $log
    status+=0
  else
    printf "\n$destinationCustomLegalFiles/css created" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationCustomLegalFiles/css already exists" | tee -a $log
  status+=0
fi

if [ ! -d "$destinationCustomLegalFiles/logos/bs/css" ]; then
  mkdir -p $destinationCustomLegalFiles/css
  if [ $? -eq 0 ]; then
    printf "\n$destinationCustomLegalFiles/logs/bs/css could not be created!" | tee -a $log
    status+=0
  else
    printf "\n$destinationCustomLegalFiles/logs/bs/css created" | tee -a $log
    status+=1
  fi
else
  printf "\n$destinationCustomLegalFiles/logs/bs/css already exists" | tee -a $log
  status+=0
fi

printf "\n\nCopying resources to destination:\n" | tee -a $log

ls $projectHome/conf/branding/resources/*.html | egrep -v 'mpdl-' | xargs -i cp {} $destinationCustomLegalFiles
if [ $? -eq 0 ]; then
  printf "\n`ls $projectHome/conf/branding/resources/*.html | egrep -v 'mpdl-'` copied to $destinationCustomLegalFiles\n" | tee -a $log
  status+=0
else
  printf "\nsome problem copying `ls $projectHome/conf/branding/resources/*.html | egrep -v 'mpdl-'` to $destinationCustomLegalFiles, this step failed!\n" | tee -a $log
  status+=1
fi

cp $projectHome/conf/branding/resources/css/*.css $destinationCustomLegalFiles
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/css/*.css copied to $destinationCustomLegalFiles" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/css/*.css to $destinationCustomLegalFiles, this step failed!" | tee -a $log
  status+=1
fi

cp -R $projectHome/conf/branding/resources/assets/* $destinationCustomLegalFiles/logos/
if [ $? -eq 0 ]; then
  printf "\n$projectHome/conf/branding/resources/assets/* copied to $destinationCustomLegalFiles/logos/" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/conf/branding/resources/assets/* to $destinationCustomLegalFiles/logos/, this step failed!" | tee -a $log
  status+=1
fi

cp -R $projectHome/src/main/webapp/resources/bs/* $destinationCustomLegalFiles/logos/bs/
if [ $? -eq 0 ]; then
  printf "\n$projectHome/src/main/webapp/resources/bs/* copied to $destinationCustomLegalFiles/logos/bs/" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/src/main/webapp/resources/bs/* to $destinationCustomLegalFiles/logos/bs/, this step failed!" | tee -a $log
  status+=1
fi

cp -R $projectHome/src/main/webapp/resources/css/structure.css $destinationCustomLegalFiles/css/
if [ $? -eq 0 ]; then
  printf "\n$projectHome/src/main/webapp/resources/css/structure.css copied to $destinationCustomLegalFiles/css/" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/src/main/webapp/resources/css/structure.css to $destinationCustomLegalFiles/css/, this step failed!" | tee -a $log
  status+=1
fi

cp -R $projectHome/src/main/webapp/resources/images/dataverse*logo.* $destinationCustomLegalFiles/logos/images
if [ $? -eq 0 ]; then
  printf "\n$projectHome/src/main/webapp/resources/images/dataverse*logo.* copied to $destinationCustomLegalFiles/logos/images" | tee -a $log
  status+=0
else
  printf "\nsome problem copying $projectHome/src/main/webapp/resources/images/dataverse*logo.* to $destinationCustomLegalFiles/logos/images, this step failed!" | tee -a $log
  status+=1
fi

#asadmin deploy $projectHome/conf/branding/guides.war

printf "\n\nSetting paths:\n\n" | tee -a $log

curl -X PUT -d " Max Planck Digital Library" $apiURL:FooterCopyright$unblock -q | tee -a $log
printf "\n" | tee -a $log

curl -X PUT -d "$customLegalFilesURL/privacy.html" $apiURL:ApplicationPrivacyPolicyUrl$unblock -q | tee -a $log
printf "\n" | tee -a $log

curl -X PUT -d "$customLegalFilesURL/terms_of_use.html" $apiURL:ApplicationTermsOfUseUrl$unblock -q | tee -a $log
printf "\n" | tee -a $log

curl -X PUT -d "$customLegalFilesURL/impressum.html" $apiURL:ApplicationDisclaimerUrl$unblock -q | tee -a $log
printf "\n" | tee -a $log

curl -X PUT -d "$customLegalFilesURL/help.html" $apiURL:NavbarGuidesUrl$unblock -q | tee -a $log
printf "\n" | tee -a $log

curl -X PUT -d@$projectHome/conf/branding/resources/mpdl-apptou-signup.html $apiURL:ApplicationTermsOfUse$unblock -q | tee -a $log
printf "\n" | tee -a $log
if [ $status -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: $status... for more info see $log\n"
  exit 2
fi
