#! /bin/bash

set -x

MAVEN=/nfs/web-hx/uniprot/software/maven/latest/bin
JAVA=/nfs/web-hx/uniprot/software/java/jdks/latest_1.8/bin/java

SIMULATION=$1
MEMORY="-Xmx$2"
PATH=$MAVEN:$JAVA:$PATH
TEST_TYPE=$3
PROFILE_PROPERTIES=""

if [ "$TEST_TYPE" == "stress" ]; then
    echo "Using stress test parameters"
    #PROFILE_PROPERTIES+=" -Da.s.host=http://wp-np2-47:8090"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wp-np2-be:8090"
    # set high parameter values here, stressful! e.g., 7000 users, 60 mins
    # for accession retrieval simulation
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.users=4000"
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=30"
    #PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=10"
    # for search and download simulation
    #PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=600"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=300"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.download.users=0"
    #PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=180"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=10"
# elif [ soak ];
    # 500 users, 2 * 24 * 60
elif [ "$TEST_TYPE" == "daily" ]; then
    echo "Using daily test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8090"
    # for accession retrieval simulation
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.users=20"
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=10"
    # for search and download simulation
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=20"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.download.users=0"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=10"
elif [ "$TEST_TYPE" == "daily-wwwdev" ]; then
    echo "Using daily test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wwwdev.ebi.ac.uk"
    # for accession retrieval simulation
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.users=20"
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=10"
    # for search and download simulation
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=20"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.download.users=5"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=10"
elif [ "$TEST_TYPE" == "newstress" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wp-np2-47:8090"
    # set high parameter values here, stressful! e.g., 7000 users, 60 mins
    # for accession retrieval simulation
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.users=3500"
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=20"
    # for search and download simulation
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=500"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.download.users=0"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=30"
elif [ "$TEST_TYPE" == "newstress2" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wp-np2-48:8090"
    # set high parameter values here, stressful! e.g., 7000 users, 60 mins
    # for accession retrieval simulation
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.users=3500"
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=30"
    # for search and download simulation
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=500"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.download.users=0"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=60"
elif [ "$TEST_TYPE" == "cloud" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://hx-rke-wp-webadmin-02-worker-9.caas.ebi.ac.uk:30299"
    # set high parameter values here, stressful! e.g., 7000 users, 60 mins
    # for accession retrieval simulation
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.users=3500"
    PROFILE_PROPERTIES+=" -Da.s.accession.retrieval.maxDuration=30"
    # for search and download simulation
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.users=300"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.download.users=0"
    PROFILE_PROPERTIES+=" -Da.s.multi.filters.maxDuration=60"
##### -----------------------------------------------------------
#####                SUPPORTING DATA PINGING
##### -----------------------------------------------------------
elif [ "$TEST_TYPE" == "daily-crossref" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8095"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/crossref-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-disease" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8095"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/disease-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-genecentric" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8095"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/genecentric-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-keyword" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8095"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/keyword-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-proteome" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8095"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/proteome-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-subcell" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://ves-hx-cb:8095"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/subcell-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-uniparc" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wwwdev.ebi.ac.uk"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/uniparc-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-uniprotkb" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wwwdev.ebi.ac.uk"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/uniprotkb-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=2"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
elif [ "$TEST_TYPE" == "daily-uniref" ]; then
    echo "Using new stress test parameters"
    PROFILE_PROPERTIES+=" -Da.s.host=http://wwwdev.ebi.ac.uk"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.list=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf/supporting-data/uniref-service-pinger.list"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.users=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.maxDuration=10"
    PROFILE_PROPERTIES+=" -Da.s.url.retrieval.successPercentGreaterThan=99"
fi

export MAVEN_OPTS="$MEMORY -Djava.net.useSystemProxies=true -Dgatling.http.ahc.readTimeout=7200000 -Dgatling.http.ahc.requestTimeout=7200000 -Dproperties.dir=/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/conf $PROFILE_PROPERTIES"
echo "Using MAVEN_OPTS=$MAVEN_OPTS"

# create new results dir
BASE_DIR="/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api"
TIMESTAMP="$(date '+%Y/%m/%d')/$(date '+%H_%M_%S')"
NEW_DIR="$TIMESTAMP-$TEST_TYPE-$SIMULATION"
TREND_JAR="$BASE_DIR/../lib/*.jar"
RESULTS_DIR="$BASE_DIR/reports/$NEW_DIR"
DAILY_TREND_DIR="$BASE_DIR/reports/trends/${TEST_TYPE}-trend"
DAILY_TREND_LOGS="$DAILY_TREND_DIR/logs/$SIMULATION"
CONF_DIR="$BASE_DIR/conf"
REPO_DIR="/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/repo/UniProt-REST-API"
echo $RESULTS_DIR
mkdir -v -p $RESULTS_DIR
mkdir -v -p $DAILY_TREND_LOGS

# update repo
cd $REPO_DIR
git pull

rsync -rv --exclude=.git /nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/repo/UniProt-REST-API $RESULTS_DIR
cd $RESULTS_DIR/UniProt-REST-API/benchmark-rest

# run maven
mvn -P benchmark clean test -Dgatling.simulationClass=$SIMULATION
TEST_SUCCESS=$?

# archive the configuration used to create the results
tar -czvf $BASE_DIR/conf.tar.gz $CONF_DIR

## capture daily trend results
#if [ "$TEST_TYPE" == "daily" -o "$TEST_TYPE" == "daily-wwwdev" ]; then
    YEAR="$(date '+%Y')"
    MONTH="$(date '+%m')"
    DAY="$(date '+%d')"

    if [ "$DAY" == "01" ]; then
        # remove simulation logs from previous month
        rm -f $DAILY_TREND_LOGS/*
    fi
    DAILY_TREND_DIR+="/$YEAR/$MONTH"

    NANOSECONDS=$(date '+%N')
    cp -v "$RESULTS_DIR"/UniProt-REST-API/benchmark-rest/target/gatling/*/*/*/simulation.log $DAILY_TREND_LOGS/simulation-${NANOSECONDS}.log
    mkdir -p "$DAILY_TREND_DIR"

    # create trend
    java -jar $TREND_JAR $DAILY_TREND_LOGS/simulation-*.log -f -o $DAILY_TREND_DIR -n ${SIMULATION}.html
#fi

# capture the results
mv -v $RESULTS_DIR/UniProt-REST-API/benchmark-rest/target/gatling $RESULTS_DIR/
mv -v $BASE_DIR/conf.tar.gz $RESULTS_DIR/

exit $TEST_SUCCESS