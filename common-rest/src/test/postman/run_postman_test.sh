#!/bin/bash
# This script is used to execute postman acceptance tests
#
# unofcal bash strict mode.
# please Refer to http://redsymbol.net/articles/unofcial-bash-strict-mode/ for details.
set -euo pipefail

case $request_type in
uniprotkb)
  echo "Running uniprotkb tests.."
  newman run ../../../../uniprotkb-rest/src/test/postman/uniprotkb.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
proteome)
  echo "Running proteome tests.."
  newman run ../../../../proteome-rest/src/test/postman/proteome.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
congure)
  echo "Running conguration tests.."
  newman run ../../../../support-data-rest/src/test/postman/congure/conguration.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
crossref)
  echo "Running crossref tests.."
  newman run ../../../../support-data-rest/src/test/postman/crossref/crossref.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
disease)
  echo "Running disease tests.."
  newman run ../../../../support-data-rest/src/test/postman/disease/disease.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
keyword)
  echo "Running keyword tests.."
  newman run ../../../../support-data-rest/src/test/postman/keyword/keyword.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
literature)
  echo "Running literature tests.."
  newman run ../../../../support-data-rest/src/test/postman/literature/literature.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
subcell)
  echo "Running subcell tests.."
  newman run ../../../../support-data-rest/src/test/postman/subcell/subcellularlocation.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
suggester)
  echo "Running suggester tests.."
  newman run ../../../../support-data-rest/src/test/postman/suggester/suggester.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
taxonomy)
  echo "Running taxonomy tests.."
  newman run ../../../../support-data-rest/src/test/postman/taxonomy/taxonomy.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
uniparc)
  echo "Running uniparc tests.."
  newman run ../../../../uniparc-rest/src/test/postman/uniparc.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
uniref)
  echo "Running uniref tests.."
  newman run ../../../../uniref-rest/src/test/postman/uniref.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
unisave)
  echo "Running unisave tests.."
  newman run ../../../../unisave-rest/src/test/postman/unisave.postman_collection.json -e "$run_environment".postman_environment.json -g uniprot_workspace.postman_globals.json
  ;;
*)
  echo "Request type '$request_type' not supported. Exiting.."
  ;;
esac
