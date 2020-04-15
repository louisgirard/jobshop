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

    public GreedySolver(Priority pr){
        this.pr = pr;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder sol = new ResourceOrder(instance);
        ArrayList<Task> feasibleTasks = new ArrayList<Task>();
        ArrayList<Task> realisees = new ArrayList<Task>();
        int numMachine = 0;
        int numTask = 0;

        //init
        for(int j = 0 ; j<instance.numJobs ; j++) {
            feasibleTasks.add(new Task(j,0));
        }
        //boucle
        while(feasibleTasks.size() != 0){
            //choisir la tache
            Task t = taskSelection(feasibleTasks,realisees,instance);
            //placer la tache sur la premiere ressource libre
            numMachine = instance.machine(t.job,t.task);
            numTask = 0;
            while(sol.matrixTask[numMachine][numTask] != null){ // si colonne vide alors on avance voir la case suivante
                numTask++;
            }
            sol.matrixTask[numMachine][numTask] = t;
            //mettre a jour les taches realisables
            realisees.add(t);
            feasibleTasks = feasibleTasks(realisees, instance);
        }
        Schedule best = sol.toSchedule();
        return new Result(instance,best,Result.ExitCause.Timeout);
    }

    private ArrayList<Task> feasibleTasks(ArrayList<Task> realisees, Instance instance){
        ArrayList<Task> feasibleTasks = new ArrayList<>();
        //parcourt de toutes les taches
        for(int t=0; t < instance.numTasks; t++){
            for(int j=0; j < instance.numJobs; j++) {
                if (!realisees.contains(new Task(j,t))){ // si la tache n est pas deja realisee
                    if(t == 0 || realisees.contains(new Task(j, t - 1))){
                        feasibleTasks.add(new Task(j,t));
                    }
                }
            }
        }
        return feasibleTasks;
    }

    private Task taskSelection(ArrayList<Task> feasibleTasks, ArrayList<Task> realisees, Instance instance){
        switch (this.pr){
            case SPT:
                return taskSelectionSPT(feasibleTasks, instance);
            case EST_SPT:
                return taskSelectionESTSPT(feasibleTasks, instance);
            case LPT:
                return taskSelectionLPT(feasibleTasks, instance);
            case SRPT:
                return taskSelectionSRPT(feasibleTasks, realisees, instance); //besoin des taches realisees pour savoir celles restantes
            case LRPT:
                return taskSelectionLRPT(feasibleTasks, realisees, instance);
            case EST_LRPT:
                return taskSelectionESTLRPT(feasibleTasks, realisees, instance);
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

    private Task taskSelectionSRPT(ArrayList<Task> feasibleTasks, ArrayList<Task> realisees, Instance instance){
        //somme des durees des taches restantes du job
        int min = Integer.MAX_VALUE;
        int dureeJob = 0;
        int job = -1;
        Task taskMin = new Task(-1,-1);
        //recherche du job avec la plus petite duree de taches restantes
        for (Task task : feasibleTasks){
            int j = task.job;
            for (int t = 0; t < instance.numTasks; t++){
                if (!realisees.contains(new Task(j,t))){ //tache non realisee = restante
                    dureeJob += instance.duration(j,t);
                }
            }
            if (dureeJob != 0 && dureeJob < min){
                min = dureeJob;
                dureeJob = 0;
                job = j;
            }
        }
        for (Task t : feasibleTasks){
            if (t.job == job) {
                taskMin = t;
                break;
            }
        }
        return taskMin;
    }
    private Task taskSelectionLRPT(ArrayList<Task> feasibleTasks, ArrayList<Task> realisees, Instance instance){
        //somme des durees des taches restantes du job
        int max = Integer.MIN_VALUE;
        int dureeJob = 0;
        int job = -1;
        Task taskMax = new Task(-1,-1);
        //recherche du job avec la plus petite duree de taches restantes
        for (Task task : feasibleTasks){
            int j = task.job;
            for (int t = 0; t < instance.numTasks; t++){
                if (!realisees.contains(new Task(j,t))){ //tache non realisee = restante
                    dureeJob += instance.duration(j,t);
                }
            }
            if (dureeJob != 0 && dureeJob > max){
                max = dureeJob;
                dureeJob = 0;
                job = j;
            }
        }
        for (Task t : feasibleTasks){
            if (t.job == job) {
                taskMax = t;
                break;
            }
        }
        return taskMax;
    }

    private Task taskSelectionESTSPT(ArrayList<Task> feasibleTasks, Instance instance){
        return taskSelectionSPT(estTasks(feasibleTasks,instance), instance);
    }

    private Task taskSelectionESTLRPT(ArrayList<Task> feasibleTasks, ArrayList<Task> realisees, Instance instance){
        return taskSelectionLRPT(estTasks(feasibleTasks,instance), realisees, instance);
    }

    //renvoie les taches realisables avec le plus petit est
    private ArrayList<Task> estTasks(ArrayList<Task> feasibleTasks, Instance instance){
        ArrayList<Task> estFeasibleTasks = new ArrayList<Task>();

        // indicate for each task that have been scheduled, its start time
        int [][] startTimes = new int [instance.numJobs][instance.numTasks];
        // for each machine, earliest time at which the machine can be used
        int[] releaseTimeOfMachine = new int[instance.numMachines];

        int bestEst = Integer.MAX_VALUE;
        //calcul des est et recherche du meilleur
        for (Task t : feasibleTasks){
            int machine = instance.machine(t.job, t.task);

            // compute the earliest start time (est) of the task
            int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
            est = Math.max(est, releaseTimeOfMachine[machine]);
            startTimes[t.job][t.task] = est;

            // increase the release time of the machine
            releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);

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
