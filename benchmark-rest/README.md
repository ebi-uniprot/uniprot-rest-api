### How to run a Gatling simulation
  - Below are two ways to run a simulation. You can pick one approach.
#### Approach 1: Run simulation `AccessionRetrievalSimulation`
```
mvn test -Pbenchmark -Dgatling.simulationClass=AsynchDownloadSimulation
```
#### Approach 2: Run simulation `FiltersWithDownloadSimulation`
```
mvn gatling:test -Dgatling.simulationClass=FiltersWithDownloadSimulation
```
- *The report will be generated in `target/gatling`.*
- *The default server being used for simulation is `http://wwwdev.ebi.ac.uk`.*

###### See the configuration in application.properties and other files in `src/test/resources` folder.

#### Each Simulation scala class is divided into 4 sections :
1. A class which extends Simulation base class
2. Common http Configuration
3. Scenario creation where we define what this scenario will do. The core of the testing.
4. Simulation setup where we configure the number of concurrent users and assert the response time and or success rate. 
