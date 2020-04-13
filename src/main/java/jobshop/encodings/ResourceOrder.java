package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

public class ResourceOrder extends Encoding {
    public Task[][] matrixTask;
    // for each machine, indicate on many tasks have been initialized
    public final int[] nextFreeSlot;

    public ResourceOrder(Instance instance){
        super(instance);

        // matrix of null elements (null is the default value of objects)
        matrixTask = new Task[instance.numMachines][instance.numJobs];

        // no task scheduled on any machine (0 is the default value)
        nextFreeSlot = new int[instance.numMachines];
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule)
    {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.matrixTask = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0 ; m<schedule.pb.numMachines ; m++) {
            final int machine = m;

            // for thi machine, find all tasks that are executed on it and sort them by their start time
            matrixTask[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

            // indicate that all tasks have been initialized for machine m
            nextFreeSlot[m] = instance.numJobs;
        }
    }

    /*
    public Schedule toSchedule() {
        // time at which each machine is going to be freed
        int[] nextFreeTimeResource = new int[instance.numMachines];

        ArrayList<Task> scheduledTasks = new ArrayList<>();

        ArrayList<Task> executableTasks;

        int[][] startTimes = new int[instance.numJobs][instance.numTasks];

        //on s'arrete quand toutes les taches sont scheduled
        while(scheduledTasks.size() != (instance.numJobs * instance.numTasks)){
            //identification des taches executables
            executableTasks = executableTasks(scheduledTasks);
            //recuperation de la premiere tache executable
            Task t = executableTasks.get(0);
            int machine = instance.machine(t.job, t.task);
            // earliest start time for this task
            int est;
            if (t.task == 0){
                est = 0;
            }else{
                est = (startTimes[t.job][t.task - 1] + instance.duration(t.job,t.task-1));
            }
            startTimes[t.job][t.task] = Math.max(est, nextFreeTimeResource[machine]);
            nextFreeTimeResource[machine] = est + instance.duration(t.job, t.task);

            //task scheduled
            scheduledTasks.add(t);
        }
        return new Schedule(instance, startTimes);
    }

    private ArrayList<Task> executableTasks(ArrayList<Task> scheduledTasks){
        ArrayList<Task> executableTasks = new ArrayList<>();
        //parcourt de toutes les taches
        for(int m=0; m < instance.numMachines; m++){
            for(int j=0; j < instance.numJobs; j++){
                // deux conditions a verifier : predecesseur sur le job scheduled et predecesseur sur la ressource (ligne machine) scheduled
                //si la tache est deja scheduled alors elle n'est pas executable
                if(!scheduledTasks.contains(matrixTask[m][j])){
                    //tache numero 0 ou predecesseur scheduled, deuxieme condition a verifier
                    if(matrixTask[m][j].task == 0 || scheduledTasks.contains(new Task(matrixTask[m][j].job, matrixTask[m][j].task - 1))){
                        //premiere tache de la ressource ou predecesseur scheduled, executable
                        if(j == 0 || scheduledTasks.contains(matrixTask[m][j-1])){
                            executableTasks.add(matrixTask[m][j]);
                        }
                    }
                }
            }
        }
        return executableTasks;
    }
     */
    @Override
    public Schedule toSchedule() {
        // indicate for each task that have been scheduled, its start time
        int [][] startTimes = new int [instance.numJobs][instance.numTasks];

        // for each job, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByJob = new int[instance.numJobs];

        // for each machine, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByMachine = new int[instance.numMachines];

        // for each machine, earliest time at which the machine can be used
        int[] releaseTimeOfMachine = new int[instance.numMachines];


        // loop while there remains a job that has unscheduled tasks
        while(IntStream.range(0, instance.numJobs).anyMatch(m -> nextToScheduleByJob[m] < instance.numTasks)) {

            // selects a task that has noun scheduled predecessor on its job and machine :
            //  - it is the next to be schedule on a machine
            //  - it is the next to be scheduled on its job
            // if there is no such task, we have cyclic dependency and the solution is invalid
            Optional<Task> schedulable =
                    IntStream.range(0, instance.numMachines) // all machines ...
                            .filter(m -> nextToScheduleByMachine[m] < instance.numJobs) // ... with unscheduled jobs
                            .mapToObj(m -> this.matrixTask[m][nextToScheduleByMachine[m]]) // tasks that are next to schedule on a machine ...
                            .filter(task -> task.task == nextToScheduleByJob[task.job])  // ... and on their job
                            .findFirst(); // select the first one if any

            if(schedulable.isPresent()) {
                // we found a schedulable task, lets call it t
                Task t = schedulable.get();
                int machine = instance.machine(t.job, t.task);

                // compute the earliest start time (est) of the task
                int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                est = Math.max(est, releaseTimeOfMachine[machine]);
                startTimes[t.job][t.task] = est;

                // mark the task as scheduled
                nextToScheduleByJob[t.job]++;
                nextToScheduleByMachine[machine]++;
                // increase the release time of the machine
                releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);
            } else {
                // no tasks are schedulable, there is no solution for this resource ordering
                return null;
            }
        }
        // we exited the loop : all tasks have been scheduled successfully
        return new Schedule(instance, startTimes);
    }

    /** Creates an exact copy of this resource order. */
    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    @Override
    public String toString() {
        String result = "";
        for(int m =0; m < instance.numMachines; m++){
            result += ("Machine " + m + " : ");
            for(int j=0; j < instance.numJobs; j++){
                result += (matrixTask[m][j] + " ");
            }
            result += "\n";
        }
        return result;
    }
}
