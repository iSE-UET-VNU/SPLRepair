package fr.inria.astor.core.ingredientbased;

import com.martiansoftware.jsap.JSAPException;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.stats.Stats;
import fr.inria.astor.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class IngredientBasedEvolutionarySPLReparApproachImpl extends IngredientBasedEvolutionaryRepairApproachImpl{

    public IngredientBasedEvolutionarySPLReparApproachImpl(MutationSupporter mutatorExecutor, ProjectRepairFacade projFacade) throws JSAPException {
        super(mutatorExecutor, projFacade);
    }


    public List<OperatorInstance> start_search_spl(int generation) throws Exception {
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
    private List<OperatorInstance> processGenerations(int generation) throws Exception {

        boolean foundOneVariant = false;

        List<OperatorInstance> temporalInstances = new ArrayList<OperatorInstance>();

        for (ProgramVariant parentVariant : variants) {

            log.debug("**Parent Variant: " + parentVariant);
            //this.saveOriginalVariant(parentVariant);
            List<ModificationPoint> modificationPointsToProcess = this.suspiciousNavigationStrategy
                    .getSortedModificationPointsList(parentVariant.getModificationPoints());
            for (ModificationPoint modificationPoint : modificationPointsToProcess) {
                log.debug("---analyzing modificationPoint position: " + modificationPoint.identified);
                modificationPoint.setProgramVariant(parentVariant);
                OperatorInstance modificationInstance = createOperatorInstanceForPoint(modificationPoint);
                if (modificationInstance != null) {

                    modificationInstance.setModificationPoint(modificationPoint);

                    log.debug("location: " + modificationPoint.getCodeElement().getPosition().getFile().getName()
                            + modificationPoint.getCodeElement().getPosition().getLine());
                    log.debug("operation: " + modificationInstance);
                    parentVariant.putModificationInstance(generation, modificationInstance);
                    temporalInstances.add(modificationInstance);

                    // We analyze all gens
                    if (!ConfigurationProperties.getPropertyBool("allpoints")) {
                        break;
                    }

                } else {// Not gen created
                    log.debug("---modifPoint  not mutation generated in  "
                            + StringUtil.trunc(modificationPoint.getCodeElement().toString()));
                }
            }
            //this.saveModifVariant(parentVariant);

        }
        return temporalInstances;
    }


    /**
     * Apply a mutation generated in previous generation to a model
     *
     * @param variant
     * @param currentGeneration
     * @throws IllegalAccessException
     */
    public void applyPreviousOperationsToVariantModel(List<OperatorInstance> operations)
            throws IllegalAccessException {

        if (operations != null && !operations.isEmpty()) {
            for (OperatorInstance genOperation : operations) {
                applyPreviousMutationOperationToSpoonElement(genOperation);
                log.debug("----gener `" + genOperation.isSuccessfulyApplied() + "`, "
                        + genOperation.toString());
            }
        }
    }


}
