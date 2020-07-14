#!/bin/bash
# This script is used to execute postman acceptance tests
#
# unofcal bash strict mode.
# please Refer to http://redsymbol.net/articles/unofcial-bash-strict-mode/ for details.
set -euo pipefail

execute_newman_run() {
  echo "Running $1 tests.."
  newman run ../../../../"$1"-rest/src/test/postman/"$1".postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
}

execute_newman_run_for_supporting_data() {
  echo "Running $1 tests.."
  newman run ../../../../support-data-rest/src/test/postman/"$1"/"$1".postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
}

if [ "$request_type" = "all" ]; then
  echo "Running all collections one after another ...."
  declare -a all_collections=('proteome' 'uniparc' 'uniprot' 'uniref' 'unisave' 'configure' 'crossref' 'disease' 'keyword'
  'literature' 'subcell' 'suggester' 'taxonomy')
  for collection in "${all_collections[@]}"; do execute_newman_run "$collection"; done
else
  case $request_type in
  uniprotkb)
    execute_newman_run uniprotkb
    ;;
  proteome)
    execute_newman_run proteome
    ;;
  uniparc)
    execute_newman_run uniparc
    ;;
  uniref)
    execute_newman_run uniref
    ;;
  unisave)
    execute_newman_run unisave
    ;;
  configure)
    execute_newman_run_for_supporting_data configure
    ;;
  crossref)
    execute_newman_run_for_supporting_data crossref
    ;;
  disease)
    execute_newman_run_for_supporting_data disease
    ;;
  keyword)
    execute_newman_run_for_supporting_data keyword
    ;;
  literature)
    execute_newman_run_for_supporting_data literature
    ;;
  subcell)
    execute_newman_run_for_supporting_data subcell
    ;;
  suggester)
    execute_newman_run_for_supporting_data suggester
    ;;
  taxonomy)
    execute_newman_run_for_supporting_data taxonomy
    ;;
  *)
    echo "Request type '$request_type' not supported. Exiting.."
    ;;
  esac
fi
