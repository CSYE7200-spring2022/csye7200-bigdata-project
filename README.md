# CSYE7200 Big Data Project

## How to start the application

#### In IntelliJ IDEA

-   (Prerequisites) Install `scala` and `Play framework` plugins
-   Open `build.sbt` as a project
-   Open `sbt` panel on the right hand side, click on `tasks -> clean` and `tasks -> compile` to recompile the project
-   Edit run configurations, add a new `Play 2 application` config, set `Play 2 Module` as `csye7200_project`
-   Run the application

## APIs

Postman collection available at `postman_api/spark.postman_collection.json`. Import it to Postman to start using.

-   **`/spark/train`**
    -   Start training the ML models and save them to files once complete
    -   Training process will be wrapped in `Future` and response will be returned immediately. Actual training status can be checked in application logs (by default `logs/application.log`)
-   **`/spark/infer/lr`**
    -   Logistic regression inference API. Input format can be checked in Postman.
    -   Output will be `0` (not popular) or `1` (popular)
-   **`/spark/infer/lr`**
    -   Random forest inference API. Input format can be checked in Postman.
    -   Output will be `0` (not popular) or `1` (popular)

## Configuration

Application configuration file is `conf/application.conf`

-   `spark` - all spark related configuration
    - `isLocal` - Boolean. Indicate whether Spark session will be run on local
    - `LRModelPath` - String. Path where logistic regression's pipeline model is saved
    - `RFModelPath` - String. Path where random forest's pipeline model is saved
    - `useCsv` - Boolean. Indicate whether we will use processed .csv file as training data input
    - `h5FolderPath` - String. If `useCsv` is set to false, all .h5 files under `h5FolderPath` will be used as training data
    - `csvPath` - String. Path where the processed .csv file is at. Only used if `useCsv` is set to true
    - `masterIp` - String. When running on cluster, the IP address of master node
    - `executorMem` - String. Memory size assigned to Spark on each cluster executor. e.g. '12g'

## Libraries

Library used for reading hdf5 file in scala: [https://github.com/jamesmudd/jhdf](https://github.com/jamesmudd/jhdf)