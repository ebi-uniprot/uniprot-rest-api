#! /bin/bash
set -e
if [ $# -ne 2 ]; then
  echo "Simulation scala class name and LSF JOB TYPE(see TEST_TYPE in go-gatling)"
  echo "Few examples: "
  echo "1. ./submit-bsub-stress.sh IdMappingSimulation stress-idmapping-kb"
  echo "2. ./submit-bsub-stress.sh URLRetrievalSimulation replay-9-May-2022-crash"
  echo "3. ./submit-bsub-stress.sh RampingURLRetrievalSimulation rampUpAndDown-uniprot-website"
  echo "4. ./submit-bsub-stress.sh IdMappingSimulation stress-idmapping"
  exit 1
fi

SIMULATION_CLASS_NAME=$1
LSF_JOB_TYPE=$2
timestamp=$(date +%s)
LSF_LOG="/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/logs/uniprot-api-gatling-${LSF_JOB_TYPE:-performance-test}-$timestamp-$(date '+%d-%b-%Y').log"
LSF_EMAIL=sahmad,lgonzales,supun,ibrahim
#LSF_CORES="8,10"
LSF_CORES="4,10"
LSF_QUEUE=production
#LSF_MEMORY=16896
LSF_MEMORY=6296
#LSF_JOB_TYPE="performance"
LSF_JOB_NAME="UniProt REST API Gatling ${LSF_JOB_TYPE:-performance} test"
LSF_R_USAGE="rusage[mem=$LSF_MEMORY]"

/ebi/lsf/codon/10.1/linux3.10-glibc2.17-x86_64/bin/bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/hps/software/users/martin/uniprot/uapi/git_repo/stress-tests/uniprot-api/bin/go-gatling $SIMULATION_CLASS_NAME 4096m $LSF_JOB_TYPE"

