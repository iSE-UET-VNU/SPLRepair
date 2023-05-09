package fr.inria.main.spl;

import fr.inria.astor.core.entities.OperatorInstance;

import java.util.ArrayList;
import java.util.List;

public class Patch {

    private List<OperatorInstance> operations = new ArrayList<>();
    private int num_of_product_successful_fix = 0;
    private int num_of_product_rejected_fix = 0;
    private List<String> succeed_products = new ArrayList<>();
    private List<String> rejected_products = new ArrayList<>();


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
    public void increase_num_of_product_successful_fix(String ploc){
        if(!succeed_products.contains(ploc)) {
            num_of_product_successful_fix += 1;
            succeed_products.add(ploc);
        }
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
    public void increase_num_of_product_rejected_fix(String ploc){
        if(!rejected_products.contains(ploc)) {
            num_of_product_rejected_fix += 1;
            rejected_products.add(ploc);
        }
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
        StringBuilder str = new StringBuilder("Num_of_operations=" + operations.size() + "\n [");
        for(OperatorInstance op:operations){
            str.append(op).append(", ");
        }
        str.append("]\n " + "num_of_product_successful_fix: ").append(num_of_product_successful_fix).append("\n").append("num_of_product_rejected_fix: ").append(num_of_product_rejected_fix).append("\n");
        str.append("List of succeed products:\n");
        for(String s:succeed_products)
            str.append(s).append("\n");
        str.append("List of rejected products:\n");
        for(String s:rejected_products)
            str.append(s).append("\n");
        return str.toString();
    }
}
