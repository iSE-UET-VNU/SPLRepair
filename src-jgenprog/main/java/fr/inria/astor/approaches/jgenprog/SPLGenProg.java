package fr.inria.astor.approaches.jgenprog;

import com.martiansoftware.jsap.JSAPException;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ProjectRepairFacade;

import java.util.List;

public class SPLGenProg extends JGenProg{
    public SPLGenProg(MutationSupporter mutatorExecutor, ProjectRepairFacade projFacade) throws JSAPException {
        super(mutatorExecutor, projFacade);
    }

    public OperatorInstance gen_an_operation_instance() throws IllegalAccessException {
        for(ProgramVariant v:variants) {
            List<ModificationPoint> modificationPointsToProcess = this.suspiciousNavigationStrategy
                    .getSortedModificationPointsList(v.getModificationPoints());
            ModificationPoint mp = modificationPointsToProcess.get(0);
            System.out.println("Trang:modification point:" + mp);
            OperatorInstance op = createOperatorInstanceForPoint(mp);
            System.out.println("Trang::operation instance::" + op + "   " + op.getModificationPoint());
            System.out.println("---------\n");
            return op;
        }
        return null;
    }
}
