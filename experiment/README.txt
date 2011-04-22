This is an experiment of the management of the plugins dependencies fully with Ivy.

To run the sample, put the jars of both easyant and ivy into sample/ea-jars.

Two special ant properties controls the task behaviour:
* easyant.refresh : set it to true to force a resolve. Otherwise the last resolve report will be used
* easyant.localrepo.basedir : set folder in which the scripts are retrieved, useful for embedding the build. If not set, the scripts are imported directly from the cache.
