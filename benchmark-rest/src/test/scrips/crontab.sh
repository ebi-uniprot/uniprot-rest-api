source_bashrc='source /nfs/public/rw/homes/uni_ci/.bashrc'
source_lsf_uniprot_benchmark_params='source /nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/lsf-params'

54 12 * * * ${source_bashrc}; export LSF_JOB_TYPE="replay-9-May-2022-crash"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling URLRetrievalSimulation 4096m $LSF_JOB_TYPE"

#15 10 * * * ${source_bashrc}; export LSF_JOB_TYPE="rampUpAndDown-uniprot-website"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling RampingURLRetrievalSimulation 4096m $LSF_JOB_TYPE"

## -------------- STRESS GATLING: rest.uniprot.org --------------
#00 4 * * SAT ${source_bashrc}; export LSF_JOB_TYPE="stress-search-result-reordering"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling URLRetrievalSimulation 4096m $LSF_JOB_TYPE"
#00 4 * * SUN ${source_bashrc}; export LSF_JOB_TYPE="stress-search-result-reordering"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling URLRetrievalSimulation 4096m $LSF_JOB_TYPE"
#00 4 * * * ${source_bashrc}; export LSF_JOB_TYPE="stress-combined-600"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling URLRetrievalSimulation 4096m $LSF_JOB_TYPE"

50 15 * * * ${source_bashrc}; export LSF_JOB_TYPE="replay-9-May-2022-crash"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling URLRetrievalSimulation 4096m $LSF_JOB_TYPE"


#04 07 * * * ${source_bashrc}; export LSF_JOB_TYPE="rampUpAndDown-uniprot-website"; ${source_lsf_uniprot_benchmark_params}; bsub -u $LSF_EMAIL -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling RampingURLRetrievalSimulation 4096m $LSF_JOB_TYPE"

## STRESS TESTING idmapping
#10 07 * * * ${source_bashrc}; export LSF_JOB_TYPE="stress-idmapping"; ${source_lsf_uniprot_benchmark_params}; bsub -u sahmad -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling IdMappingSimulation 4096m $LSF_JOB_TYPE"

#13 09 * * * ${source_bashrc}; export LSF_JOB_TYPE="stress-idmapping-kb"; ${source_lsf_uniprot_benchmark_params}; bsub -u sahmad -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling IdMappingSimulation 4096m $LSF_JOB_TYPE"

#13 09 * * * ${source_bashrc}; export LSF_JOB_TYPE="stress-idmapping-uniparc"; ${source_lsf_uniprot_benchmark_params}; bsub -u sahmad -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling IdMappingSimulation 4096m $LSF_JOB_TYPE"

#13 09 * * * ${source_bashrc}; export LSF_JOB_TYPE="stress-idmapping-uniref"; ${source_lsf_uniprot_benchmark_params}; bsub -u sahmad -n $LSF_CORES -o $LSF_LOG -N -q $LSF_QUEUE -M $LSF_MEMORY -J "$LSF_JOB_NAME" -R $LSF_R_USAGE "/nfs/public/rw/homes/uni_ci/stress-tests/uniprot-api/bin/go-gatling IdMappingSimulation 4096m $LSF_JOB_TYPE"