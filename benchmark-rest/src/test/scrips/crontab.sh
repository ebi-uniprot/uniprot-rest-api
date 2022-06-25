source_bashrc='source /nfs/public/rw/homes/uni_ci/.bashrc'
source_lsf_uniprot_benchmark_params='source /nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/lsf-params'

54 12 * * * ${source_bashrc}; export LSF_JOB_TYPE="replay-9-May-2022-crash"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling URLRetrievalSimulation 4096m $LSF_JOB_TYPE"

#15 10 * * * ${source_bashrc}; export LSF_JOB_TYPE="rampUpAndDown-uniprot-website"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling RampingURLRetrievalSimulation 4096m $LSF_JOB_TYPE"