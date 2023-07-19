Automatic Program Repair for Variability Bugs in Software Product Line Systems
========================================
This is build upon Astor,  an automatic software repair framework in Java for Java, done by Inria, the University of Lille, the Université Polytechnique Hauts-de-France, and KTH Royal Institute of Technology. 


Installation
------

* Following [this instruction]([https://github.com/SpoonLabs/astor/blob/master/docs/getting-starting.md](https://github.com/ttrangnguyen/SPLRepair/blob/SPLRepair/docs/getting-starting.md)https://github.com/ttrangnguyen/SPLRepair/blob/SPLRepair/docs/getting-starting.md) to install the repair tool


How to execute
------

For executing the enhanced version of system-based approach with the employed APR tool cardumen:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode cardumen -location <PATH_TO_SPLSYSTEM_FOLDER>/SPL/ExamDB/ -flresult varcop_fl_results.txt -approachdirection systembased -approachvariant enhanced

For executing the enhanced version of system-based approach with the employed APR tool jgenprog:

    java -cp target/astor-*-jar-with-dependencies.jar fr.inria.main.evolution.SPLRepairMain -mode jgenprog -location <PATH_TO_SPLSYSTEM_FOLDER>/SPL/ExamDB/ -flresult varcop_fl_results.txt -approachdirection systembased -approachvariant enhanced


Command line arguments:
------

-location: is the absolute path to the folder of the buggy SPL systems
-mode: the employed APR tool supported by Astor, i.e., jgenprog|cardumen|jmutrepair|jkali|tibra
-flresult: fault localization result 
-approachdirection: the direction for repairing SPL system. i.e., productbased|systembased. The default direction is productbased
-approachvariant: the enhanced or basic variant of the approach, i.e., enhanced|basic. The default variant is basic
For the other arguments, please check [here]([https://github.com/ttrangnguyen/SPLRepair/blob/SPLRepair/src/main/resources/astor.properties])

Dataset of variability bugs:
------
https://tuanngokien.github.io/splc2021/









