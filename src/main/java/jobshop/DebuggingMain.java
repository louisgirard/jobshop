package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.solvers.GreedySolver;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {

    public static void main(String[] args) {
        try {
            // load the aaa1 instance
            Instance instance = Instance.fromFile(Paths.get("instances/aaa1"));

            // construit une solution dans la représentation par
            // numéro de jobs : [0 1 1 0 0 1]
            // Note : cette solution a aussi été vue dans les exercices (section 3.3)
            //        mais on commençait à compter à 1 ce qui donnait [1 2 2 1 1 2]
            JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;

            System.out.println("\nENCODING: " + enc);

            Schedule sched = enc.toSchedule();
            System.out.println("SCHEDULE: " + sched);

            ResourceOrder res = new ResourceOrder(sched);
            System.out.println("Resource order from schedule " + res);

            sched = res.toSchedule();
            System.out.println("SCHEDULE: " + sched);

            // TEST SOLVEUR
            System.out.println("----------Test Greedy Solver----------");
            Instance inst = Instance.fromFile(Paths.get("instances/aaa1"));
            for (GreedySolver.Priority pr : GreedySolver.Priority.values()){
                GreedySolver greedySolver = new GreedySolver(pr);
                Result result = greedySolver.solve(inst,10);

                //System.out.println("Greedy solver: " + result.schedule);;
                System.out.println("Greedy solver " + pr);
                System.out.println("makespan: " + result.schedule.makespan());

            }



        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
