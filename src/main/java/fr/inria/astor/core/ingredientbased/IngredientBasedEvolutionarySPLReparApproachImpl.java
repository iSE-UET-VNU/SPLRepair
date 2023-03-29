package fr.inria.astor.core.ingredientbased;

import com.martiansoftware.jsap.JSAPException;
import fr.inria.astor.core.antipattern.AntiPattern;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.stats.Stats;

import java.util.ArrayList;
import java.util.List;

public class IngredientBasedEvolutionarySPLReparApproachImpl extends IngredientBasedEvolutionaryRepairApproachImpl{

    public IngredientBasedEvolutionarySPLReparApproachImpl(MutationSupporter mutatorExecutor, ProjectRepairFacade projFacade) throws JSAPException {
        super(mutatorExecutor, projFacade);
    }


    public List<ProgramVariant> start_search_spl(int generation) throws Exception {
        log.info("----Starting SPL Repair Evolutionary Search");
        return processGenerations(generation);
    }

    /**
     * Process a generation i: loops over all instances
     *
     * @param generation
     * @return
     * @throws Exception
     */
    private List<ProgramVariant> processGenerations(int generation) throws Exception {

        boolean foundOneVariant = false;

        List<ProgramVariant> temporalInstances = new ArrayList<ProgramVariant>();

        currentStat.increment(Stats.GeneralStatEnum.NR_GENERATIONS);

        for (ProgramVariant parentVariant : variants) {

            log.debug("**Parent Variant: " + parentVariant);


            this.saveOriginalVariant(parentVariant);
            ProgramVariant newVariant = createNewProgramVariant(parentVariant, generation);
            this.saveModifVariant(parentVariant);

            if (newVariant == null) {
                continue;
            }

            boolean solution = false;

            if (ConfigurationProperties.getPropertyBool("antipattern")) {
                if (!AntiPattern.isAntiPattern(newVariant, generation)) {
                    temporalInstances.add(newVariant);
                    solution = processCreatedVariant(newVariant, generation);
                }
            } else {
                temporalInstances.add(newVariant);
                //solution = processCreatedVariant(newVariant, generation);
            }

//            if (solution) {
//                foundSolution = true;
//                newVariant.setBornDate(new Date());
//            }
            foundOneVariant = true;
            // Finally, reverse the changes done by the child
            reverseOperationInModel(newVariant, generation);
            //boolean validation = this.validateReversedOriginalVariant(newVariant);

//            if (solution) {
//                this.savePatch(newVariant);
//                if(!this.successed_operators.contains(newVariant.getOperations().get(generation).get(0)))
//                    this.successed_operators.add( newVariant.getOperations().get(generation).get(0));
//
//            }else{
//                if(!this.rejected_operators.contains(newVariant.getOperations().get(generation).get(0)))
//                    this.rejected_operators.add(newVariant.getOperations().get(generation).get(0));
//            }

//            if (foundSolution && ConfigurationProperties.getPropertyBool("stopfirst")) {
//                break;
//            }

        }
        //prepareNextGeneration(temporalInstances, generation);

        if (!foundOneVariant)
            this.nrGenerationWithoutModificatedVariant++;
        else {
            this.nrGenerationWithoutModificatedVariant = 0;
        }

        return temporalInstances;
    }
}
