package fr.inria.astor.approaches.splgenprog;

import com.martiansoftware.jsap.JSAPException;

import fr.inria.astor.core.ingredientbased.IngredientBasedEvolutionarySPLReparApproachImpl;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.main.evolution.ExtensionPoints;

/**
 * Core repair approach based on reuse of ingredients.
 *
 * @author Trang Nguyen, trang.nguyen@vnu.edu.vn
 *
 */
public class SPLGenProg extends IngredientBasedEvolutionarySPLReparApproachImpl {

    public SPLGenProg(MutationSupporter mutatorExecutor, ProjectRepairFacade projFacade) throws JSAPException {
        super(mutatorExecutor, projFacade);
        setPropertyIfNotDefined(ExtensionPoints.OPERATORS_SPACE.identifier, "irr-statements");

        setPropertyIfNotDefined(ExtensionPoints.TARGET_CODE_PROCESSOR.identifier, "statements");
    }
}
