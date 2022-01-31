#!/bin/bash
#
# The purpose of this script is to deploy custom legal documents for Dataverse
# issue-15 on github repository
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

STATIC_PAGES_URL="http://${HOST_URL}/guides"

API_URL="http://${HOST_URL}/api/admin/settings/"
UNBLOCK="?unblock-key=blkAPI_dev_ed2"
#
#################################################
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

LOG="/tmp/$(basename ${0}).$(date +'%s').log";

printf "\nLegal Documents Installation (process output to -> ${LOG})\n" | tee -a ${LOG}

printf "\n\nPreparing destination folders:\n" | tee -a ${LOG}

if [ ! -d "${STATIC_PAGES}/logos" ]; then
  mkdir -p ${STATIC_PAGES}
  if [ $? -eq 0 ]; then
    printf "\n${STATIC_PAGES}/logos created" | tee -a ${LOG}
    status+=0
  else
    printf "\n${STATIC_PAGES}/logos could not be created!" | tee -a ${LOG}
    exit 1
  fi
else
  printf "\n${STATIC_PAGES}/logos already exists" | tee -a ${LOG}
  status+=0
fi

printf "\n\nCopying resources to destination:\n" | tee -a ${LOG}

ls "${PROYECT_HOME}/conf/branding/resources/"*.html | egrep -v 'mpdl-' | xargs -i cp {} ${STATIC_PAGES}
if [ $? -eq 0 ]; then
  printf "\n$(ls ${PROYECT_HOME}/conf/branding/resources/*.html | egrep -v 'mpdl-') copied to ${STATIC_PAGES}\n" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying $(ls ${PROYECT_HOME}/conf/branding/resources/*.html | egrep -v 'mpdl-') to ${STATIC_PAGES}, this step failed!\n" | tee -a ${LOG}
  status+=1
fi

cp -RT "${PROYECT_HOME}/conf/branding/resources/assets" "${STATIC_PAGES}/logos"
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/conf/branding/resources/assets/* copied to ${STATIC_PAGES}/logos" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/conf/branding/resources/assets/* to ${STATIC_PAGES}/logos, this step failed!" | tee -a ${LOG}
  status+=1
fi

cp -R "${PROYECT_HOME}/src/main/webapp/resources/bs" "${STATIC_PAGES}/logos"
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/src/main/webapp/resources/bs copied to ${STATIC_PAGES}/logos" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/src/main/webapp/resources/bs to ${STATIC_PAGES}/logos, this step failed!" | tee -a ${LOG}
  status+=1
fi

cp "${PROYECT_HOME}/src/main/webapp/resources/css/structure.css" "${STATIC_PAGES}/css"
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/src/main/webapp/resources/css/structure.css copied to ${STATIC_PAGES}/css" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/src/main/webapp/resources/css/structure.css to ${STATIC_PAGES}/css, this step failed!" | tee -a ${LOG}
  status+=1
fi

cp "${PROYECT_HOME}/src/main/webapp/resources/images/"dataverse*logo.* "${STATIC_PAGES}/logos/images"
if [ $? -eq 0 ]; then
  printf "\n${PROYECT_HOME}/src/main/webapp/resources/images/dataverse*logo.* copied to ${STATIC_PAGES}/logos/images" | tee -a ${LOG}
  status+=0
else
  printf "\nsome problem copying ${PROYECT_HOME}/src/main/webapp/resources/images/dataverse*logo.* to ${STATIC_PAGES}/logos/images, this step failed!" | tee -a ${LOG}
  status+=1
fi

printf "\n\nDeploying static pages:\n\n" | tee -a ${LOG}

${PAYARA_HOME}/glassfish/bin/asadmin list-applications | grep "guides"
if [ $? -eq 1 ]; then
  current = $(pwd)
  cd ${PROYECT_HOME}/conf/branding/guides; jar cvf ../guides.war .
  cd $current
  ${PAYARA_HOME}/glassfish/bin/asadmin deploy ${PROYECT_HOME}/conf/branding/guides.war
fi

printf "\n\nSetting paths:\n\n" | tee -a ${LOG}

curl -X PUT -d " Max Planck Digital Library" ${API_URL}:FooterCopyright${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

curl -X PUT -d "${STATIC_PAGES_URL}/privacy.html" ${API_URL}:ApplicationPrivacyPolicyUrl${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

curl -X PUT -d "${STATIC_PAGES_URL}/terms_of_use.html" ${API_URL}:ApplicationTermsOfUseUrl${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

curl -X PUT -d "${STATIC_PAGES_URL}/impressum.html" ${API_URL}:ApplicationDisclaimerUrl${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

curl -X PUT -d "${STATIC_PAGES_URL}/help.html" ${API_URL}:NavbarGuidesUrl${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

curl -X PUT -d@${PROYECT_HOME}/conf/branding/resources/mpdl-apptou-signup.html ${API_URL}:ApplicationTermsOfUse${UNBLOCK} -q | tee -a ${LOG}
printf "\n" | tee -a ${LOG}

if [ ${status} -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: ${status}... for more info see ${LOG}\n"
  exit 2
fi
