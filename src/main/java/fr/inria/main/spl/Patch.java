package fr.inria.main.spl;

import fr.inria.astor.core.entities.OperatorInstance;

public class Patch {

    private OperatorInstance op;
    private int num_of_product_successful_fix = 0;
    private int num_of_product_rejected_fix = 0;


    public Patch(OperatorInstance _op){
        op = _op;
    }

    public void setOp(OperatorInstance op) {
        this.op = op;
    }

    public OperatorInstance getOp() {
        return op;
    }

    public int getNum_of_product_successful_fix() {
        return num_of_product_successful_fix;
    }

    public void setNum_of_product_successful_fix(int num_of_product_successful_fix) {
        this.num_of_product_successful_fix = num_of_product_successful_fix;
    }
    public void increase_num_of_product_successful_fix(){
        num_of_product_successful_fix += 1;
    }
    public void decrease_num_of_product_successful_fix(){
        num_of_product_successful_fix -= 1;
    }

    public int getNum_of_product_rejected_fix() {
        return num_of_product_rejected_fix;
    }

    public void setNum_of_product_rejected_fix(int num_of_product_rejected_fix) {
        this.num_of_product_rejected_fix = num_of_product_rejected_fix;
    }
    public void increase_num_of_product_rejected_fix(){
        num_of_product_rejected_fix += 1;
    }
    public void decrease_num_of_product_rejected_fix(){
        num_of_product_rejected_fix -= 1;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null) return false;
        if(!this.getClass().equals(obj.getClass())) return false;
        Patch other = (Patch) obj;
        return op.equals(other.getOp());
    }

    @Override
    public String toString(){
        return op.toString() + "\n" + "num_of_product_successful_fix: " + num_of_product_successful_fix + "\n" +
                "num_of_product_rejected_fix: " + num_of_product_rejected_fix + "\n";
    }
}
