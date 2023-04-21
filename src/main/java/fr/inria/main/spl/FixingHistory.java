package fr.inria.main.spl;

import fr.inria.astor.core.entities.OperatorInstance;

public class FixingHistory {
    OperatorInstance op;
    boolean succeedfix;

    public FixingHistory(){
        op = null;
        succeedfix = false;
    }
    public FixingHistory(OperatorInstance _op, boolean _succeedfix){
        op = _op;
        succeedfix = _succeedfix;
    }

    public OperatorInstance getOp() {
        return op;
    }
    public boolean isSucceedfix(){
        return succeedfix;
    }
    @Override
    public boolean equals(Object obj){
        if(obj == null) return false;
        if(obj instanceof FixingHistory){
            FixingHistory other = (FixingHistory) obj;
            return op.equals(other.getOp());
        }
        return false;
    }
    @Override
    public String toString(){
        if(succeedfix)
            return op + " has successfully fixed the modification point: " + op.getModificationPoint();
        else
            return op + " cannot fix the modification point: " + op.getModificationPoint();
    }
}
