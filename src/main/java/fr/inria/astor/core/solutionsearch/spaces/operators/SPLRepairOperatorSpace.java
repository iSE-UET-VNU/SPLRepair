package fr.inria.astor.core.solutionsearch.spaces.operators;

import fr.inria.astor.core.entities.SuspiciousModificationPoint;

public class SPLRepairOperatorSpace extends UniformRandomRepairOperatorSpace{
    public SPLRepairOperatorSpace(OperatorSpace space) {
        super(space);
    }

    @Override
    public AstorOperator getNextOperator(SuspiciousModificationPoint modificationPoint) {
        System.out.println("Trang::SPL get next operator");
        //If we decide to mutate the point according to its suspiciousness value
        if(mutateModificationPoint(modificationPoint)){
            //here, this strategy does not take in account the modifpoint to select the op.
            return this.getNextOperator();
        }
        else{
            //We dont mutate the modif point
            return null;
        }


    }
}
