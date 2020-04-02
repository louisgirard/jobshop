package jobshop.encodings;

import jobshop.Instance;
import jobshop.Schedule;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class ResourceOrder {
    public final Instance instance;
    public Task[][] matrixTask;
    public Schedule sched;

    public ResourceOrder(Instance instance){
        this.instance = instance;
        this.matrixTask = new Task[instance.numMachines][instance.numJobs];
        for(int m =0; m < instance.numMachines; m++){
            for(int j=0; j < instance.numJobs; j++){
                matrixTask[m][j] = new Task(-1,-1);
            }
        }
    }

    public void fromSchedule(Schedule sched){
        this.matrixTask = new Task[instance.numMachines][instance.numJobs];
        this.sched = sched;

        for(int m =0; m < instance.numMachines; m++){
            for(int j=0; j < instance.numJobs; j++){
                matrixTask[m][j] = new Task(j,instance.task_with_machine(j,m));
            }
            sortByStartTime(matrixTask,m);
        }
    }

    private void sortByStartTime(Task[][] matrixTask, int m){
        int n = instance.numJobs;
        for (int i = 0; i < n-1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (sched.startTime(j, matrixTask[m][j].task) > sched.startTime(j + 1, matrixTask[m][j + 1].task)) {
                    Task temp = matrixTask[m][j];
                    matrixTask[m][j] = matrixTask[m][j + 1];
                    matrixTask[m][j + 1] = temp;
                }
            }
        }
    }

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
