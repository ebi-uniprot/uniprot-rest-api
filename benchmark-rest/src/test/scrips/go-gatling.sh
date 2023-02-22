#! /bin/bash

set -x

MAVEN=/hps/software/spack/opt/spack/linux-centos8-sandybridge/gcc-9.3.0/maven-3.6.3-yg2w4dpna655fsmrdvy2bpoaawz7l63j/bin
JAVA=/hps/software/users/martin/uniprot/jdks/latest_1.11/bin/java
#export JAVA_HOME=/nfs/web-hx/uniprot/software/java/jdks/latest_1.8

SIMULATION=$1
MEMORY="-Xmx$2"
PATH=$MAVEN:$JAVA:$PATH
TEST_TYPE=$3
PROFILE_PROPERTIES=""

if [ "$TEST_TYPE" == "stress-combined-600" ]; then
    echo "Using new stress combined test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/conf/combined/all-general-terms.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=300"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=300"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=80"
elif [ "$TEST_TYPE" == "stress-search-result-reordering" ]; then
    echo "Stressing more accurate boosts, i.e., changing scoring => more computation on Solr side"
    PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-2.caas.ebi.ac.uk:31244"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/conf/general-terms-20K.list2"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=300"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=120"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=80"
elif [ "$TEST_TYPE" == "download-to-stress-voldemort" ]; then
    echo "Stressing Voldemort"
    PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-4.caas.ebi.ac.uk:32235"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/conf/stressing-voldemort/default-search-genes-20K.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=500"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=360"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=80"
elif [ "$TEST_TYPE" == "replay-9-May-2022-crash" ]; then
    echo "Stressing Whole REST/Solr/Voldemort"
   # PROFILE_PROPERTIES+=" -Da.s.host=http://hh-rke-wp-webadmin-02-worker-7.caas.ebi.ac.uk:32450" # nginx prod
   PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-1.caas.ebi.ac.uk:31393" # nginx fb
    #PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-2.caas.ebi.ac.uk:30134"
     #PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
    #PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-master-1.caas.ebi.ac.uk:30822"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/conf/going-live/2022-05-09-all-api.requests.gatling"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/homes/uni_ci/GET-browser-resource_tm_uniprot_stream.txt.3"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/mixedSearchStream.txt"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/mixedAll.txt"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/filtered-transformed-GET-browser-programmatic"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_adm/tmp/log_parsing/sql/get-200-transformed-prepared-for-gatling.not-distinct.txt"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/requestsNoStream.txt"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/2022.gatling.modified5"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/nobackup/production/uniprot/logs/2022v3/2022.gatling"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/final.txt"
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/nonkb.gatling"
      PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/mixedStreamOnly.txt"
      #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/nonkbcleanmixed.txt"
      #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/unirefonly.txt"
      #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/onlyaccessions.txt"
     # PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/idmappingresults.txt"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/nobackup/production/uniprot/logs/2022v3/gatling/all.gatling"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=100" # works
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=2000" # 1% HTTP 5xx mostly 502
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=400" # 1.5% 5xx
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=3000" # 0.02% 500 ran for 30 mins
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=3000" #
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=4000" # i.n.c.ChannelException: Failed to open a socket. 16% need to fix gatling
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=1000" # works okay
     #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=1300" # works okay
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=3000" # not great -- leads to KB rest app spiral of death crashing
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=30"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=720"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=540"
   # PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=60"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=80"
elif [ "$TEST_TYPE" == "rampUpAndDown-uniprot-website" ]; then
    echo "Long stressing whole of REST/Solr/Voldemort"
    #PROFILE_PROPERTIES+=" -Da.s.host=http://hh-rke-wp-webadmin-02-worker-7.caas.ebi.ac.uk:32450" # nginx prod
    #PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-1.caas.ebi.ac.uk:30134" # nginx fb
    PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/nonkb.gatling"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/mixedAllNew.txt"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/nobackup/production/uniprot/logs/2022v3/2022.gatling"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/nonkbcleanmixed.txt"
   PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/final.txt"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_adm/tmp/log_parsing/sql/get-200-transformed-prepared-for-gatling.txt"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_adm/tmp/log_parsing/sql/get-200-transformed-prepared-for-gatling.not-distinct.txt"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/filtered-transformed-GET-browser-programmatic"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/onlyKBRequests.txt"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/onlyKBRequestsNoStream.txt"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/nonkb.txt"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=1000"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=5000"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.constantRPS=40"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.constantRPS=50"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.constantRPS=100"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.constantRPS=300"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.constantRPSDuration=60"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.constantRPSDuration=90"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxRPS=200"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxRPS=400"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxRPSDuration=10"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxRPSDuration=10"
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=170" # works 3.4% 500
    #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=170"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=100"
   #PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=200"
   # PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=500"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=80"
elif [ "$TEST_TYPE" == "stress-idmapping" ]; then
    echo "Stressing idmapping"
    #PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
    PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-1.caas.ebi.ac.uk:30134" # nginx fb
    #PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-master-1.caas.ebi.ac.uk:30822"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.from=UniProtKB_AC-ID"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.to=UniProtKB"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.run.url=/idmapping/run"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.status.url=/idmapping/status"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.results.url=/idmapping/uniprotkb/results"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.results.params=\"facets=reviewed,model_organism,proteins_with,existence,annotation_score,length\""
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.users=50"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.idCount=3000"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.users=25"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.idCount=5000"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.users=10"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.idCount=10000"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.accessions.csv=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/accessions.csv"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.maxDuration=30"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.successPercentGreaterThan=80"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.percentile3.responseTime=500"
elif [ "$TEST_TYPE" == "stress-idmapping-kb" ]; then
    echo "Stressing idmapping kb"
#    PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-1.caas.ebi.ac.uk:30134" # nginx fb
    PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.from=UniProtKB_AC-ID"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.to=UniProtKB"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.run.url=/idmapping/run"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.status.url=/idmapping/status"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.results.url=/idmapping/uniprotkb/results/stream"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.results.params=facets\=reviewed,model_organism,proteins_with,existence,annotation_score,length"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.users=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.idCount=10"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.users=2"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.idCount=5"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.users=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.idCount=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.accessions.csv=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/accessions.csv"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.maxDuration=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.successPercentGreaterThan=80"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.percentile3.responseTime=500"
elif [ "$TEST_TYPE" == "stress-idmapping-uniparc" ]; then
    echo "Stressing idmapping"
    PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.from=UniProtKB_AC-ID"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.to=UniParc"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.run.url=/idmapping/run"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.status.url=/idmapping/status"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.results.url=/idmapping/uniparc/results"
    #PROFILE_PROPERTIES+=" -Da.s.idmapping.results.params=\"facets=reviewed,model_organism,proteins_with,existence,annotation_score,length\""
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.users=2"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.idCount=100"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.users=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.idCount=500"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.users=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.idCount=1000"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.accessions.csv=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/accessions.csv"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.maxDuration=60"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.successPercentGreaterThan=80"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.percentile3.responseTime=500"
elif [ "$TEST_TYPE" == "stress-idmapping-uniref" ]; then
    echo "Stressing idmapping"
    PROFILE_PROPERTIES+=" -Da.s.host=http://rest.uniprot.org"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.from=UniProtKB_AC-ID"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.to=UniRef100"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.run.url=/idmapping/run"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.status.url=/idmapping/status"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.results.url=/idmapping/uniref/results"
    #PROFILE_PROPERTIES+=" -Da.s.idmapping.results.params=\"facets=reviewed,model_organism,proteins_with,existence,annotation_score,length\""
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.users=2"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.idCount=100"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.users=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario2.idCount=500"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.users=1"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario3.idCount=1000"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.accessions.csv=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/accessions.csv"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.maxDuration=60"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.successPercentGreaterThan=80"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.percentile3.responseTime=500"
elif [ "$TEST_TYPE" == "stress-async-download-kb" ]; then
    echo "Stressing async download uniprotkb"
    PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-35-worker-9.caas.ebi.ac.uk:32007"
    PROFILE_PROPERTIES+=" -Da.s.nginx.host=http://hx-rke-wp-webadmin-35-worker-9.caas.ebi.ac.uk:31210"
    PROFILE_PROPERTIES+=" -Da.s.asynch.download.run.url=/uniprotkb/download/run"
    PROFILE_PROPERTIES+=" -Da.s.asynch.download.status.url=/uniprotkb/download/status"
    PROFILE_PROPERTIES+=" -Da.s.asynch.download.ids.url=/uniprotkb/download/ids"
    PROFILE_PROPERTIES+=" -Da.s.asynch.download.results.url=/uniprotkb/download/results"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.scenario1.users=2"
    PROFILE_PROPERTIES+=" -Da.s.asynch.download.query.list=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/testreq/asynchdownload1.txt"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.maxDuration=5"
    PROFILE_PROPERTIES+=" -Da.s.idmapping.successPercentGreaterThan=90"
fi

export MAVEN_OPTS="$MEMORY -Djava.net.useSystemProxies=true -Dgatling.http.requestTimeout=7200000 -Dproperties.dir=/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/conf $PROFILE_PROPERTIES"
echo "Using MAVEN_OPTS=$MAVEN_OPTS"

# create new results dir
BASE_DIR="/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api"
TIMESTAMP="$(date '+%Y/%m/%d')/$(date '+%H_%M_%S')"
NEW_DIR="$TIMESTAMP-$TEST_TYPE-$SIMULATION"
TREND_JAR="$BASE_DIR/../lib/*.jar"
RESULTS_DIR="$BASE_DIR/reports/$NEW_DIR"
DAILY_TREND_DIR="$BASE_DIR/reports/trends/${TEST_TYPE}-trend"
DAILY_TREND_LOGS="$DAILY_TREND_DIR/logs/$SIMULATION"
CONF_DIR="$BASE_DIR/conf"
REPO_DIR="/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/repo/UniProt-REST-API"
echo $RESULTS_DIR
mkdir -v -p $RESULTS_DIR
mkdir -v -p $DAILY_TREND_LOGS

# update repo
cd $REPO_DIR
git pull

rsync -rv --exclude=.git /hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/repo/UniProt-REST-API $RESULTS_DIR
cd $RESULTS_DIR/UniProt-REST-API/benchmark-rest

# run maven
mvn -P benchmark clean test -Dgatling.simulationClass=$SIMULATION | tee app.log
TEST_SUCCESS=${PIPESTATUS[0]}

# archive the configuration used to create the results
tar -czvf $BASE_DIR/conf.tar.gz $CONF_DIR

## capture daily trend results
#if [ "$TEST_TYPE" == "daily" -o "$TEST_TYPE" == "daily-wwwdev" ]; then
#    YEAR="$(date '+%Y')"
#    MONTH="$(date '+%m')"
#    DAY="$(date '+%d')"
#
#    if [ "$DAY" == "01" ]; then
#        # remove simulation logs from previous month
#        rm -f $DAILY_TREND_LOGS/*
#    fi
#    DAILY_TREND_DIR+="/$YEAR/$MONTH"
#
#    NANOSECONDS=$(date '+%N')
#    cp -v "$RESULTS_DIR"/UniProt-REST-API/benchmark-rest/target/gatling/*/*/*/simulation.log $DAILY_TREND_LOGS/simulation-${NANOSECONDS}.log
#    for l in `ls $DAILY_TREND_LOGS/simulation-*.log`; do
#        if [ ! -s $l ] ; then
#            echo "[WARN] removing empty log file"
#            rm -v $l
#        fi
#    done
#    mkdir -p "$DAILY_TREND_DIR"
#
#    # create trend
#    $JAVA -jar $TREND_JAR $DAILY_TREND_LOGS/simulation-*.log -f -o $DAILY_TREND_DIR -n ${SIMULATION}.html
#fi


# capture the results
#mv -v $RESULTS_DIR/UniProt-REST-API/benchmark-rest/target/gatling $RESULTS_DIR/
mv -v $RESULTS_DIR/benchmark-rest/target/gatling $RESULTS_DIR/
mv -v app.log $RESULTS_DIR/
mv -v $BASE_DIR/conf.tar.gz $RESULTS_DIR/

exit $TEST_SUCCESS