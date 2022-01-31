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
usage="usage: ${0} [-k|--key <unblock-key>]"
if [[ ${#} -gt 0 ]]; then
  key="$1"
  case $key in
    -k|--key)
      if [[ ${#} -eq 2 ]]; then
        UNBLOCK="?unblock-key=${2}"
      else
        printf "\n%s\n" "${usage}"
        exit 1
      fi
      ;;
    *)
        printf "\n%s\n" "${usage}"
      exit 1
      ;;
  esac
fi

LOG="/tmp/$(basename "${0}").$(date +'%s').log";

printf "\nLegal Documents Installation (process output to -> %s)\n" "${LOG}"| tee -a ${LOG}

printf "\n\nPreparing destination folders:\n" | tee -a "${LOG}"

function create_dir {
  if [ ! -d "${1}" ]; then
    mkdir -p "${1}"
    if [ ${?} -eq 0 ]; then
      printf "\n%screated" "${1}" | tee -a "${LOG}"
      status+=0
    else
      printf "\n%s could not be created!" "${1}" | tee -a "${LOG}"
      exit 1
    fi
  else
    printf "\n%s already exists" "${1}" | tee -a "${LOG}"
    status+=0
  fi
}

create_dir "${STATIC_PAGES}/logos"

printf "\n\nCopying resources to destination:\n" | tee -a "${LOG}"

function copy_from_to_with_flags {
    cp  ${3} "${1}" "${2}"
    if [ ${?} -eq 0 ]; then
      printf "\n%s copied to %s" "${1}" "${2}" | tee -a "${LOG}"
      status+=0
    else
      printf "\nsome problem copying %s to %s, this step failed!" "${1}" "${2}" | tee -a "${LOG}"
      status+=1
    fi
}

for item in $(find ${PROYECT_HOME}/conf/branding/resources/*.html -type f \( ! -name 'mpdl-*' \))
do
  copy_from_to_with_flags ${item} ${STATIC_PAGES}
done
copy_from_to_with_flags "${PROYECT_HOME}/conf/branding/resources/assets" "${STATIC_PAGES}/logos" "-RT"
copy_from_to_with_flags "${PROYECT_HOME}/src/main/webapp/resources/bs" "${STATIC_PAGES}/logos" "-R"
copy_from_to_with_flags "${PROYECT_HOME}/src/main/webapp/resources/css/structure.css" "${STATIC_PAGES}/css"
for item in $(find ${PROYECT_HOME}/src/main/webapp/resources/images/dataverse*logo.* -type f)
do
  copy_from_to_with_flags ${item} "${STATIC_PAGES}/logos/images"
done

printf "\n\nDeploying static pages:\n\n" | tee -a "${LOG}"

${PAYARA_HOME}/glassfish/bin/asadmin list-applications | grep "guides"
if [ ${?} -eq 1 ]; then
  current=$(pwd)
  cd ${PROYECT_HOME}/conf/branding/guides; jar cvf ../guides.war .
  cd $current
  ${PAYARA_HOME}/glassfish/bin/asadmin deploy ${PROYECT_HOME}/conf/branding/guides.war
fi

printf "\n\nSetting paths:\n\n" | tee -a "${LOG}"

curl -X PUT -d " Max Planck Digital Library" ${API_URL}:FooterCopyright"${UNBLOCK}" -q | tee -a "${LOG}"
printf "\n" | tee -a "${LOG}"
curl -X PUT -d "${STATIC_PAGES_URL}/privacy.html" ${API_URL}:ApplicationPrivacyPolicyUrl"${UNBLOCK}" -q | tee -a "${LOG}"
printf "\n" | tee -a "${LOG}"
curl -X PUT -d "${STATIC_PAGES_URL}/terms_of_use.html" ${API_URL}:ApplicationTermsOfUseUrl"${UNBLOCK}" -q | tee -a "${LOG}"
printf "\n" | tee -a "${LOG}"
curl -X PUT -d "${STATIC_PAGES_URL}/impressum.html" ${API_URL}:ApplicationDisclaimerUrl"${UNBLOCK}" -q | tee -a "${LOG}"
printf "\n" | tee -a "${LOG}"
curl -X PUT -d "${STATIC_PAGES_URL}/help.html" ${API_URL}:NavbarGuidesUrl"${UNBLOCK}" -q | tee -a "${LOG}"
printf "\n" | tee -a "${LOG}"
curl -X PUT -d@${PROYECT_HOME}/conf/branding/resources/mpdl-apptou-signup.html ${API_URL}:ApplicationTermsOfUse"${UNBLOCK}" -q | tee -a "${LOG}"
printf "\n" | tee -a "${LOG}"

if [ ${status} -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: %s... for more info see %s\n" "${status}" "${LOG}"
  exit 2
fi

}