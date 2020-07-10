
To run a Postman test
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

2. ./run_postman_test.sh

 
To edit a collection.json in Postman
- Load the collection in Postman application by clicking import button on top left
- Edit accordingly
- Export the collection and replace this collection at the appropriate place

To import the environment.json in Postman
- Click on top right cog-wheel icon to `Manage Environment`
- import the required environment.json file