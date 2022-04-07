#!/bin/bash
#
#set -ev; #error fixing
#set -xv; #debug
#set -nv; #check syntax
#
function help {
   cat << EOF

   The purpose of this script is to customize dataverse branding for MPDL.
   For more info see:
    https://github.com/MPDL/dataverse/issues/7
    https://github.com/MPDL/dataverse/issues/15

   Usage: $(basename "${BASH_SOURCE[0]}") [-h] [-k unblock-key] [-p proyect-home]
   Available options:
      -h        Print this Help.
      -k key    API unblock-key.
      -p path   Path to proyect files.

EOF
  exit
}
UNBLOCK=""
PROYECT_HOME=$(cd "../.."; pwd)
while getopts ":hk:p:" option; do
   case $option in
      h) help
         exit;;
      k) UNBLOCK="?unblock-key=${OPTARG}";;
      p) PROYECT_HOME=$OPTARG;;
     \?) # Invalid option
         echo "Invalid option (try option -h for help)"
         exit;;
   esac
done

PAYARA_HOME="/srv/web/payara5"
export STATIC_PAGES="/srv/mpdl-dataverse/custom"
case $(hostname) in
  vm97)
    isPROD=false
    HOST_URL="dev-edmond2.mpdl.mpg.de"
    API_PROTOCOL="https"
    ;;
  vm64)
    isPROD=true
    HOST_URL="edmond.mpdl.mpg.de"
    API_PROTOCOL="https"
    ;;
  *)
    isPROD=false
    HOST_URL="localhost:8080"
    API_PROTOCOL="http"
    PAYARA_HOME=$(whereis payara5 | awk '{print $2}')
    ;;
esac

if [ "${UNBLOCK}" == "" ]; then
  if [ ${isPROD} == "true" ]; then
    echo "Option -k key (API unblock-key) needed!"
    exit 1
  else
    UNBLOCK="?unblock-key=blkAPI_dev_ed2"
  fi
fi
readonly DOCROOT="${PAYARA_HOME}/glassfish/domains/domain1/docroot"
readonly STATIC_PAGES_URL="http://${HOST_URL}/guides"
readonly API_URL="${API_PROTOCOL}://${HOST_URL}/api/admin/settings/"
readonly LOG="/tmp/$(basename "${0}").$(date +'%s').log";
export LOG;

printf "\n\nBranding Installation (process output to -> %s)\n" "${LOG}" | tee -a "${LOG}"

printf "\n\n Preparing destination folders:\n" | tee -a "${LOG}"
function create_dir {
  if [ ! -d "${1}" ]; then
    if mkdir -p "${1}"; then
      printf "\n\t %s created" "${1}" | tee -a "${LOG}"
      status+=0
    else
      printf "\n\t %s could not be created!" "${1}" | tee -a "${LOG}"
      exit 1
    fi
  else
    printf "\n\t %s already exists" "${1}" | tee -a "${LOG}"
    status+=0
  fi
}
# issue-7
create_dir "${DOCROOT}/logos"
create_dir "${STATIC_PAGES}"
# issue-15
create_dir "${STATIC_PAGES}/logos"

printf "\n\n Copying resources to destination:\n" | tee -a "${LOG}"
function copy_from_to_with_flags {
    if cp ${3} "${1}" "${2}";
    then
      printf "\n\t %s copied to %s" "${1}" "${2}" | tee -a "${LOG}"
      status+=0
    else
      printf "\n\t some problem copying %s to %s, this step failed!" "${1}" "${2}" | tee -a "${LOG}"
      status+=1
    fi
}
export -f copy_from_to_with_flags
# issue-7
copy_from_to_with_flags "${PROYECT_HOME}/conf/branding/resources/assets" "${DOCROOT}/logos" "-RT"
copy_from_to_with_flags "${PROYECT_HOME}/conf/branding/resources/css" ${STATIC_PAGES} "-R"
copy_from_to_with_flags "${PROYECT_HOME}/conf/branding/resources/mpdl-footer.html" ${STATIC_PAGES}
# issue-15
find "${PROYECT_HOME}/conf/branding/resources/"*.html -type f \( ! -name 'mpdl-*' \) -exec bash -c 'copy_from_to_with_flags ${1} ${STATIC_PAGES}' -- {} \;
copy_from_to_with_flags "${PROYECT_HOME}/conf/branding/resources/assets" "${STATIC_PAGES}/logos" "-RT"
copy_from_to_with_flags "${PROYECT_HOME}/conf/branding/resources/js" ${STATIC_PAGES} "-R"
copy_from_to_with_flags "${PROYECT_HOME}/src/main/webapp/resources/bs" "${STATIC_PAGES}/logos" "-R"
copy_from_to_with_flags "${PROYECT_HOME}/src/main/webapp/resources/css/structure.css" "${STATIC_PAGES}/css"
copy_from_to_with_flags "${PROYECT_HOME}/src/main/webapp/resources/images/fav/edmond_favicon_red.svg" "${STATIC_PAGES}/logos/images/fav" "-R"
find "${PROYECT_HOME}/src/main/webapp/resources/images/"dataverse*logo.* -type f -exec bash -c 'copy_from_to_with_flags ${1} ${STATIC_PAGES}/logos/images' -- {} \;

# issue-15
printf "\n\n Static pages server status:\n\n\t" | tee -a "${LOG}"
"${PAYARA_HOME}"/glassfish/bin/asadmin list-applications | grep "guides" | tee -a "${LOG}"
if [ "${PIPESTATUS[0]}" -eq 1 ]; then
  printf "\n\t Deploying static pages:\n" | tee -a "${LOG}"
  current=$(pwd)
  cd "${PROYECT_HOME}/conf/branding/guides" || return; jar cvf ../guides.war . ; cd "${current}" || exit
  "${PAYARA_HOME}"/bin/asadmin deploy "${PROYECT_HOME}/conf/branding/guides.war" | tee -a "${LOG}"
else
  printf "\t Deployed\n" | tee -a "${LOG}"
fi

printf "\n\n Configuring Settings on Database:\n\n" | tee -a "${LOG}"
function db_setting_to {
  if [[ ! "${1}" =~ [^:] ]]
  then
    setting=:"${1}"
  else
    setting="${1}"
  fi
  curl -X PUT -d "${2}" "${API_URL}${setting}${UNBLOCK}" --silent | jq '.' | tee -a "${LOG}"
  if [ "${PIPESTATUS[0]}" -eq 0 ]; then
    status+=0
  else
    status+=1
  fi
  printf "\n" | tee -a "${LOG}"
}
# issue-7
db_setting_to ":LogoCustomizationFile" "/logos/navbar/logo_for_bright.png"
db_setting_to ":FooterCustomizationFile" "${STATIC_PAGES}/mpdl-footer.html"
db_setting_to ":StyleCustomizationFile" "${STATIC_PAGES}/css/mpdl-stylesheet.css"
# issue-15
readonly COPYRIGHT=" Max Planck Digital Library"
db_setting_to ":FooterCopyright" "${COPYRIGHT}"
db_setting_to ":ApplicationPrivacyPolicyUrl" "${STATIC_PAGES_URL}/privacy.html"
db_setting_to ":ApplicationTermsOfUseUrl" "${STATIC_PAGES_URL}/terms_of_use.html"
db_setting_to ":ApplicationDisclaimerUrl" "${STATIC_PAGES_URL}/impressum.html"
db_setting_to ":NavbarGuidesUrl" "${STATIC_PAGES_URL}/help.html"
db_setting_to ":ApplicationTermsOfUse" "@${PROYECT_HOME}/conf/branding/resources/mpdl-apptou-signup.html"
#curl -X GET  ${API_URL}?unblock-key=blkAPI_dev_ed2  | jq '.'

if [ ${status} -eq 0 ]; then
  printf "\n... DONE!\n"
  exit 0
else
  printf "\n... sorry, some step fails!\nsome hints: %s... for more info see %s\n" "${status}" "${LOG}"
  exit 2
fi