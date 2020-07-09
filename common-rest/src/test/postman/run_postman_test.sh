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

if [ "$request_type" == "configure" ]; then
	echo "Running configuration tests.."
	newman run ../../../../support-data-rest/src/test/postman/configure/configuration.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "crossref" ]; then
	echo "Running crossref tests.."
	newman run ../../../../support-data-rest/src/test/postman/crossref/crossref.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "disease" ]; then
	echo "Running disease tests.."
	newman run ../../../../support-data-rest/src/test/postman/disease/disease.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "keyword"  ]; then
	echo "Running keyword tests.."
	newman run ../../../../support-data-rest/src/test/postman/keyword/keyword.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "literature" ]; then
	echo "Running literature tests.."
	newman run ../../../../support-data-rest/src/test/postman/literature/literature.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "subcell" ]; then
	echo "Running subcell tests.."
	newman run ../../../../support-data-rest/src/test/postman/subcell/subcellularlocation.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "suggester" ]; then
	echo "Running suggester tests.."
	newman run ../../../../support-data-rest/src/test/postman/suggester/suggester.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "taxonomy" ]; then
	echo "Running taxonomy tests.."
	newman run ../../../../support-data-rest/src/test/postman/taxonomy/taxonomy.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "uniparc" ]; then
	echo "Running uniparc tests.."
	newman run ../../../../uniparc-rest/src/test/postman/uniparc.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "uniref" ]; then
	echo "Running uniref tests.."
	newman run ../../../../uniref-rest/src/test/postman/uniref.postman_collection.json -e integration.postman_environment.json
fi

if [ "$request_type" == "unisave" ]; then
	echo "Running unisave tests.."
	newman run ../../../../unisave-rest/src/test/postman/unisave.postman_collection.json -e integration.postman_environment.json
fi
