Automated Program Repair for Variability Bugs in Software Product Line Systems
========================================


Installation
------

* Following [this instruction](https://github.com/SpoonLabs/astor/blob/master/docs/getting-starting.md](https://github.com/ttrangnguyen/SPLRepair/blob/SPLRepair/docs/getting-starting.md)https://github.com/ttrangnguyen/SPLRepair/blob/SPLRepair/docs/getting-starting.md) to install the repair tool


How to execute
------

For executing the enhanced version of multi-product-based approach with the employed APR tool cardumen:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SPL/ExamDB/ -flresult varcop_fl_results.txt -approachdirection systembased -approachvariant enhanced

For executing the enhanced version of multi-product-based approach with the employed APR tool jgenprog:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode jgenprog -location <PATH_TO_SPLSYSTEM_FOLDER>/SPL/ExamDB/ -flresult varcop_fl_results.txt -approachdirection systembased -approachvariant enhanced


Command line arguments:
------

-location: is the absolute path to the folder of the buggy SPL systems

-mode: the employed APR tool supported by Astor, i.e., jgenprog|cardumen|jmutrepair|jkali|tibra

-flresult: fault localization result 

-approachdirection: the direction for repairing SPL system. i.e., productbased|systembased. The default direction is productbased

-approachvariant: the enhanced or basic variant of the approach, i.e., enhanced|basic. The default variant is basic

For the other arguments, please check [here](https://github.com/ttrangnguyen/SPLRepair/blob/SPLRepair/src/main/resources/astor.properties)

Dataset of variability bugs:
------
https://tuanngokien.github.io/splc2021/

How to reproduce the experiments:
------

RQ1:
-

For executing the single-product-based approach with cardumen:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SYSTEM_NAME/ -flresult varcop_fl_results.txt 

For executing the multi-product-based approach with cardumen:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SYSTEM_NAME/ -flresult varcop_fl_results.txt -approachdirection systembased

To execute the experiments with jgenprog, please replace the -mode parameter with jgenprog, for example to execute the multi-product-based approach with jgenprog:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode jgenprog -location <PATH_TO_SPLSYSTEM_FOLDER>/SYSTEM_NAME/ -flresult varcop_fl_results.txt -approachdirection systembased

To execute the repair approaches with the heuristic rules, please add the parameter  -approachvariant enhanced, for example: 

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode jgenprog -location <PATH_TO_SPLSYSTEM_FOLDER>/SPL/ExamDB/ -flresult varcop_fl_results.txt -approachdirection systembased -approachvariant enhanced

RQ2: 
-

To change the similarity function (e.g., cosine, ngram, lcs, levenshtein, jaccard), add the parameters -similarityfunc. The default similarity function is levenshtein.

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SYSTEM_NAME/ -flresult varcop_fl_results.txt -approachvariant enhanced -similarityfunc cosine

To change the suitability threshold, add the parameters -suitabilitythreshold. The default value is 0.5.

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SYSTEM_NAME/ -flresult varcop_fl_results.txt -approachvariant enhanced -similarityfunc cosine -suitabilitythreshold 0.9

To change the hyperparameters alpha and beta, add the parameters -similarityalpha and -similaritybeta. The default value of alpha is 2 and the default value of beta is 1.

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SYSTEM_NAME/ -flresult varcop_fl_results.txt -approachvariant enhanced -similarityalpha 1 -similaritybeta 0
