#!/bin/bash
#
# The purpose of this script is to deploy custom branding
# issue-7 on github repository
#
#################################################
# PLEASE, TAKE A LOOK AT VARIABLES BEFORE RUN !!!
#################################################
#
set -x;
readonly PROYECT_HOME="../.."
PAYARA_HOME="/srv/web/payara5"
readonly STATIC_PAGES="/srv/mpdl-dataverse/custom"

case $(hostname) in
  vm97)
    HOST_URL="dev-edmond2.mpdl.mpg.de"
    ;;
  vm64)
    HOST_URL="prod-edmond2.mpdl.mpg.de"
    ;;
  *)
    HOST_URL="localhost:8080"
    PAYARA_HOME=$(whereis payara5 | awk '{print $2}')
    ;;
esac

DOCROOT="${PAYARA_HOME}/glassfish/domains/domain1/docroot"

API_URL="http://${HOST_URL}/api/admin/settings/"
UNBLOCK="?unblock-key=blkAPI_dev_ed2"
#
#################################################

usage="usage: $0 [-k|--key <unblock-key>]"
if [[ ${#} -gt 0 ]]; then
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

LOG="/tmp/$(basename ${0}).$(date +'%s').log";

printf "\n\nBranding Installation (process output to -> ${LOG})\n" | tee -a ${LOG}

printf "\n\nPreparing destination folders:\n" | tee -a ${LOG}

if [ ! -d "${DOCROOT}/logos" ]; then
  mkdir -p ${DOCROOT}/logos
  if [ $? -eq 0 ]; then
    printf "\n${DOCROOT}/logos created" | tee -a ${LOG}
    status="0"
  else
    printf "\n${DOCROOT}/logos could not be created!" | tee -a ${LOG}
    exit 1
  fi
else
  printf "\n${DOCROOT}/logos already exists" | tee -a ${LOG}
  status="0"
fi

if [ ! -d "${STATIC_PAGES}" ]; then
  mkdir -p ${STATIC_PAGES}
  if [ $? -eq 0 ]; then
    printf "\n${STATIC_PAGES} created" | tee -a ${LOG}
    status+=0
  else
    printf "\n${STATIC_PAGES} could not be created!" | tee -a ${LOG}
    exit 1
  fi
else
  printf "\n${STATIC_PAGES} already exists" | tee -a ${LOG}
  status+=0
fi

printf "\n\nCopying resources to destination:\n" | tee -a ${LOG}

cp -RT "${PROYECT_HOME}/conf/branding/resources/assets" "${DOCROOT}/logos"
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/conf/branding/resources/assets/* copied to ${DOCROOT}/logos" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/conf/branding/resources/assets/* to ${DOCROOT}/logos, this step failed!" | tee -a ${LOG}
  status+=1
fi

cp -R "${PROYECT_HOME}/conf/branding/resources/css" ${STATIC_PAGES}
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/conf/branding/resources/css copied to ${STATIC_PAGES}" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/conf/branding/resources/css to ${STATIC_PAGES}, this step failed!" | tee -a ${LOG}
  status+=1
fi

cp "${PROYECT_HOME}/conf/branding/resources/mpdl-footer.html" ${STATIC_PAGES}
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/conf/branding/resources/assets/mpdl-footer.html copied to ${STATIC_PAGES}" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/conf/branding/resources/assets/mpdl-footer.html to ${STATIC_PAGES}, this step failed!" | tee -a ${LOG}
  status+=1
fi

printf "\n\nSetting paths:\n\n" | tee -a ${LOG}

curl -X PUT -d "/logos/navbar/logo_for_bright.png" ${API_URL}:LogoCustomizationFile${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}
curl -X PUT -d "${STATIC_PAGES}/mpdl-footer.html" ${API_URL}:FooterCustomizationFile${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}
curl -X PUT -d "${STATIC_PAGES}/css/mpdl-stylesheet.css" ${API_URL}:StyleCustomizationFile${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

if [ ${status} -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: ${status}... for more info see ${LOG}\n"
  exit 2
fi
