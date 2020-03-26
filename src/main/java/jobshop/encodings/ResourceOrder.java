package jobshop.encodings;

import jobshop.Instance;
import jobshop.Schedule;

import java.util.Arrays;

public class ResourceOrder {
    public final Instance pb;
    Task[][] matrixTask;
    public Schedule sched;

    public ResourceOrder(Instance pb, Schedule sched){
        this.pb = pb;
        this.matrixTask = new Task[pb.numMachines][pb.numJobs];
        this.sched = sched;

        for(int m =0; m < pb.numMachines; m++){
            for(int j=0; j < pb.numJobs; j++){
                matrixTask[m][j] = new Task(j,pb.task_with_machine(j,m));
            }
            sortByStartTime(matrixTask,m);
        }
    }

    private void sortByStartTime(Task[][] matrixTask, int m){
        int n = pb.numJobs;
        for (int i = 0; i < n-1; i++)
            for (int j = 0; j < n-i-1; j++)
                if (sched.startTime(j,matrixTask[m][j].task) > sched.startTime(j+1,matrixTask[m][j+1].task))
                {
                    Task temp = matrixTask[m][j];
                    matrixTask[m][j] = matrixTask[m][j+1];
                    matrixTask[m][j+1] = temp;
                }
    }

    @Override
    public String toString() {
        String result = "";
        for(int m =0; m < pb.numMachines; m++){
            result += ("Machine " + m + " : ");
            for(int j=0; j < pb.numJobs; j++){
                result += (matrixTask[m][j] + " ");
            }
            result += "\n";
        }
        return result;
    }
}
