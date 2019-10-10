#!/bin/bash
# This script is used to execute postman acceptance tests
#
# unoffical bash strict mode.
# please Refer to http://redsymbol.net/articles/unofficial-bash-strict-mode/ for details.
set -euo pipefail

if [ "$run_uniprotkb_tests" == "true" ]; then
	echo "Running uniprotkb tests!"
	newman run UniProtAPIs.postman_collection.json -e "$run_environment".postman_environment.json --folder uniprotkb
fi

if [ "$run_keyword_tests" == "true" ]; then
	echo "Running keyword tests!"
	newman run UniProtAPIs.postman_collection.json -e "$run_environment".postman_environment.json --folder keyword
fi
