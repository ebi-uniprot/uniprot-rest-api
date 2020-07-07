#!/bin/bash
# This script is used to execute postman acceptance tests
#
# unoffical bash strict mode.
# please Refer to http://redsymbol.net/articles/unofficial-bash-strict-mode/ for details.
set -euo pipefail

if [ "$request_type" == "uniprotkb" ]; then
	echo "Running uniprotkb tests.."
	newman run ../../../../uniprotkb-rest/src/test/postman/uniprotkb.postman_collection.json -e integration.postman_environment.json
fi
if [ "$request_type" == "proteome" ]; then
	echo "Running proteome tests.."
	newman run ../../../../proteome-rest/src/test/postman/proteome.postman_collection.json -e integration.postman_environment.json
fi