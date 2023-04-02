package fr.inria.main.spl;

import fr.inria.astor.core.entities.OperatorInstance;

import java.util.ArrayList;
import java.util.List;

public class Patch {

    private List<OperatorInstance> operations = new ArrayList<>();
    private int num_of_product_successful_fix = 0;
    private int num_of_product_rejected_fix = 0;


    public Patch(List<OperatorInstance> _op){
        operations = _op;
    }

    public Patch() {

    }

    public void setOperations(List<OperatorInstance> operations) {
        this.operations = operations;
    }

    public List<OperatorInstance> getOperations() {
        return operations;
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
        return operations.equals(other.getOperations());
    }

    @Override
    public String toString(){
        String str = "Num_of_operations=" + operations.size() + "\n [";
        for(OperatorInstance op:operations){
            str += op + ", ";
        }
        str += "]\n " + "num_of_product_successful_fix: " + num_of_product_successful_fix + "\n" +
                "num_of_product_rejected_fix: " + num_of_product_rejected_fix + "\n";
        return str;
    }
}
