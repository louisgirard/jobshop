package jobshop.solvers;

import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;
import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class GreedySolver implements Solver {
    public enum Priority {SPT, LPT, SRPT, LRPT, EST_SPT, EST_LRPT};

    private Priority pr;

    // indicate for each task its start time
    int [][] startTimes;

    // for each machine, earliest time at which the machine can be used
    int[] releaseTimeOfMachine;

    public GreedySolver(Priority pr){
        this.pr = pr;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder sol = new ResourceOrder(instance);
        ArrayList<Task> feasibleTasks = new ArrayList<Task>();

        // indicate for each task its start time
        startTimes = new int [instance.numJobs][instance.numTasks];

        // for each machine, earliest time at which the machine can be used
        releaseTimeOfMachine = new int[instance.numMachines];

        int numMachine = 0;
        int numTask = 0;

        //init
        for(int j = 0 ; j<instance.numJobs ; j++) {
            feasibleTasks.add(new Task(j,0));
        }
        //boucle
        while(feasibleTasks.size() != 0){
            //choisir la tache
            Task t = taskSelection(feasibleTasks,instance);
            releaseTimeOfMachine[instance.machine(t.job,t.task)] = startTimes[t.job][t.task] + instance.duration(t.job, t.task);
            //placer la tache sur la premiere ressource libre
            numMachine = instance.machine(t.job,t.task);
            numTask = 0;
            while(sol.matrixTask[numMachine][numTask] != null){ // si colonne vide alors on avance voir la case suivante
                numTask++;
            }
            sol.matrixTask[numMachine][numTask] = t;
            //mettre a jour les taches realisables
            updatefeasibleTasks(t,feasibleTasks,instance);
        }
        Schedule best = sol.toSchedule();
        return new Result(instance,best,Result.ExitCause.Timeout);
    }

    private void updatefeasibleTasks(Task task, ArrayList<Task> feasibleTasks, Instance instance){
        //si on n'est pas sur la derniere tache alors on remplace par la suivante
        if (task.task != (instance.numTasks - 1)){
            //feasibleTasks.add(new Task(task.job,task.task + 1));
            feasibleTasks.set(feasibleTasks.indexOf(task),new Task(task.job,task.task + 1));
        }else{// sinon on la supprime
            feasibleTasks.remove(task);

        }
    }

    private Task taskSelection(ArrayList<Task> feasibleTasks, Instance instance){
        switch (this.pr){
            case SPT:
                return taskSelectionSPT(feasibleTasks, instance);
            case EST_SPT:
                return taskSelectionESTSPT(feasibleTasks, instance);
            case LPT:
                return taskSelectionLPT(feasibleTasks, instance);
            case SRPT:
                return taskSelectionSRPT(feasibleTasks, instance);
            case LRPT:
                return taskSelectionLRPT(feasibleTasks, instance);
            case EST_LRPT:
                return taskSelectionESTLRPT(feasibleTasks, instance);
            default:
                return feasibleTasks.get(0);
        }
    }

    private Task taskSelectionSPT(ArrayList<Task> feasibleTasks, Instance instance){
        int min = Integer.MAX_VALUE;
        Task taskMin = new Task(-1,-1);
        for (Task t : feasibleTasks){
            if (instance.duration(t.job,t.task) < min) {
                min = instance.duration(t.job,t.task);
                taskMin = t;
            }
        }
        return taskMin;
    }

    private Task taskSelectionLPT(ArrayList<Task> feasibleTasks, Instance instance){
        int max = Integer.MIN_VALUE;
        Task taskMax = new Task(-1,-1);
        for (Task t : feasibleTasks){
            if (instance.duration(t.job,t.task) > max) {
                max = instance.duration(t.job,t.task);
                taskMax = t;
            }
        }
        return taskMax;
    }

    // on regarde toutes les taches apres celle en cours
    private int remainingTime(Task task, Instance instance){
        int duree = 0;
        for(int t = task.task; t < instance.numTasks; t++){
            duree += instance.duration(task.job,t);
        }
        return duree;
    }

    private Task taskSelectionSRPT(ArrayList<Task> feasibleTasks, Instance instance){
        //somme des durees des taches restantes du job
        int min = Integer.MAX_VALUE;
        Task taskMin = null;
        //recherche de la tache avec la plus petite duree de taches restantes
        for (Task task : feasibleTasks){
            if (remainingTime(task,instance) < min){
                min = remainingTime(task,instance);
                taskMin = new Task(task.job,task.task);
            }
        }
        return taskMin;
    }
    private Task taskSelectionLRPT(ArrayList<Task> feasibleTasks, Instance instance){
        //somme des durees des taches restantes du job
        int max = Integer.MIN_VALUE;
        Task taskMax = null;
        //recherche de la tache avec la plus grande duree de taches restantes
        for (Task task : feasibleTasks){
            if (remainingTime(task,instance) > max){
                max = remainingTime(task,instance);
                taskMax = new Task(task.job,task.task);
            }
        }
        return taskMax;
    }

    private Task taskSelectionESTSPT(ArrayList<Task> feasibleTasks, Instance instance){
        return taskSelectionSPT(estTasks(feasibleTasks,instance), instance);
    }

    private Task taskSelectionESTLRPT(ArrayList<Task> feasibleTasks, Instance instance){
        return taskSelectionLRPT(estTasks(feasibleTasks,instance), instance);
    }

    //renvoie les taches realisables avec le plus petit est
    private ArrayList<Task> estTasks(ArrayList<Task> feasibleTasks, Instance instance){
        ArrayList<Task> estFeasibleTasks = new ArrayList<Task>();

        int bestEst = Integer.MAX_VALUE;
        //calcul des est et recherche du meilleur
        for (Task t : feasibleTasks){
            int machine = instance.machine(t.job, t.task);
            // compute the earliest start time (est) of the task
            int est;
            if(t.task == 0){
                est = 0;
            } else{
                est = startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
            }
            est = Math.max(est, releaseTimeOfMachine[machine]);
            startTimes[t.job][t.task] = est;

            if (est < bestEst){
                bestEst = est;
            }
        }
        //recherche des taches avec le meilleur est
        for (Task t : feasibleTasks){
            if (startTimes[t.job][t.task] == bestEst){
                estFeasibleTasks.add(t);
            }
        }
        return estFeasibleTasks;
    }
}
