
####Run Postman tests from command line:
1. export run_environment=\<environment\>
    - Supported environments:
        - integration
        - production
        - local 
2. export request_type=\<type\>
   - Supported request types:
     - uniprotkb 
     - proteome
     - configure
     - crossref
     - disease
     - keyword
     - literature
     - subcell
     - suggester 
     - taxonomy
     - uniparc
     - uniref
     - unisave        

3. ./run_postman_test.sh

####Run/Edit the Postmant tests in Postman application:
 
1. Import a collection.json (`e.g. ../../../../uniprotkb-rest/src/test/postman/uniprotkb.postman_collection.json`) in Postman
    - Load the collection in Postman application by clicking import button on top left
    - Edit accordingly
    - Export the collection and replace this collection at the appropriate place

2. Import the environment.json in Postman
    - Click on top right cog-wheel icon to `Manage Environment`
    - import the required environment.json file

3. To import the global variables stored in uniprot_workspace.postman_globals.json in Postman
    - Click on top right cog-wheel icon to `Manage Environment`
    - import the *globals.json file
