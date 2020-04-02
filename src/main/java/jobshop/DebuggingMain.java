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
            System.out.println("VALID: " + sched.isValid());
            System.out.println("MAKESPAN: " + sched.makespan());

            ResourceOrder res = new ResourceOrder(sched.pb);
            res.fromSchedule(sched);
            System.out.println("Resource order from schedule " + res);

            sched = res.toSchedule();
            System.out.println("SCHEDULE: " + sched);

            GreedySolver greedySolver = new GreedySolver(GreedySolver.Priority.LPT);
            Result result = greedySolver.solve(instance,10);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
