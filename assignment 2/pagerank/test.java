import java.util.*;
import java.io.*;


public class test {

	final static double BORED = 0.15;

    static HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

    public test() {
        this.link.put(0, new HashMap<Integer, Boolean>());
        this.link.get(0).put(1, true);
        this.link.get(0).put(2, true);
        this.link.get(0).put(3, true);

        this.link.put(1, new HashMap<Integer, Boolean>());
        this.link.get(1).put(3, true);

        this.link.put(2, new HashMap<Integer, Boolean>());
        this.link.get(2).put(3, true);
        this.link.get(2).put(4, true);

        this.link.put(3, new HashMap<Integer, Boolean>());
        this.link.get(3).put(4, true);

        this.link.put(4, new HashMap<Integer, Boolean>());

    }

    public static void main(String[] args) {

        int numberOfDocs = 5;

        test test = new test();

        // double[] x = new double[numberOfDocs];
        // for (int i = 0; i < numberOfDocs; i++)
        //     x[i] = 1.0 / numberOfDocs;
        double[] x = new double[numberOfDocs];
        x[0] = 1.0;
        double[] result = test.get_new(x, numberOfDocs);
        for(int i=0;i<18;i++){
            for(int j=0;j<result.length;j++) System.out.print(String.valueOf(result[j])+" "); print("");
            result = test.get_new(result, numberOfDocs);
        }
    }

    public double[] get_new(double[] x, int numberOfDocs) {
		double[] result = new double[numberOfDocs];

        for(int target_node=0;target_node<numberOfDocs;target_node++){
            double[] column = new double[numberOfDocs];

            for(int node=0;node<numberOfDocs;node++){
                if(this.link.get(node).isEmpty()){
                    column[node] += 1/(double)numberOfDocs;
                } else if(this.link.get(node).containsKey(target_node)){
                    column[node] += (1-BORED)/this.link.get(node).size() + BORED/numberOfDocs;
                } else{
                    column[node] += BORED/numberOfDocs;
                }
            }
            result[target_node] = this.dot(x,column,numberOfDocs);
        }

        // double sum = 0;
		// for (int i = 0; i < numberOfDocs; i++)
		// 	sum += result[i];
		// for (int i = 0; i < numberOfDocs; i++)
		// 	result[i] /= sum;
		return result;
	}

    public double dot(double[] x1, double[] x2, int numberOfDocs) {
		double res = 0;
		for (int i = 0; i < numberOfDocs; i++) {
			res += x1[i] * x2[i];
		}
		return res;
	}


    public static void print(Object o) {
        System.out.println(o);
    }
}