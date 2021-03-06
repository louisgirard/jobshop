package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.solvers.DescentSolver;
import jobshop.solvers.GreedySolver;
import jobshop.solvers.TabooSolver;
import sun.jvm.hotspot.opto.Block;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DebuggingMain {

    public static void main(String[] args) {
        try {/*
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
            Instance inst = Instance.fromFile(Paths.get("instances/ft06"));
            for (GreedySolver.Priority pr : GreedySolver.Priority.values()){
                GreedySolver greedySolver = new GreedySolver(pr);
                Result result = greedySolver.solve(inst,10);

                System.out.println("Greedy solver " + pr);
                System.out.println("makespan: " + result.schedule.makespan());

            }
            */

            /*
            // TEST SOLVEUR
            System.out.println("----------Test Descent Solver----------");
            Instance instance1 = Instance.fromFile(Paths.get("instances/aaa1"));
            DescentSolver descentSolver = new DescentSolver();
            long debut = System.currentTimeMillis();
            Result result = descentSolver.solve(instance1,10);

            System.out.println("Descent solver ");
            System.out.println(result.schedule);
            System.out.println("makespan: " + result.schedule.makespan());
            System.out.println("Temps exec : " + (System.currentTimeMillis()-debut));
            */

            // TEST SOLVEUR

            System.out.println("----------Test Taboo Solver----------");
            Instance instance1 = Instance.fromFile(Paths.get("instances/ft20"));
            TabooSolver tabooSolver = new TabooSolver(50, 5);
            long debut = System.currentTimeMillis();
            Result result = tabooSolver.solve(instance1,300);

            System.out.println("Taboo solver ");
            System.out.println(result.schedule);
            System.out.println("makespan: " + result.schedule.makespan());
            System.out.println("Temps exec : " + (System.currentTimeMillis()-debut));


            Instance instance = Instance.fromFile(Paths.get("instances/ft06"));
            int res = 0;
            for (int j = 0 ; j < instance.numJobs ; j++){
                for (int t = 0; t < instance.numTasks; t++){
                    res += instance.duration(j,t);
                }
            }
            System.out.println(res);
            System.out.println("n : " + instance.numJobs + ", m : " + instance.numMachines);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
