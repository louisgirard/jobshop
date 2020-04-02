package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class GreedySolver implements Solver {
    public enum Priority {SPT, LPT, SRPT, LRPT};

    private Priority pr;

    public GreedySolver(Priority pr){
        this.pr = pr;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder sol = new ResourceOrder(instance);
        ArrayList<Task> feasibleTasks = new ArrayList<Task>();
        ArrayList<Task> realise = new ArrayList<>();
        int numMatrice = 0;
        int numTask = 0;

        //init
        for(int j = 0 ; j<instance.numJobs ; j++) {
            feasibleTasks.add(new Task(j,0));
        }
        //boucle
        while(feasibleTasks.size() != 0){
            //choisir la tache, au debut on prend la premiere tache
            Task t = feasibleTasks.get(0);
            //placer la tache sur la premiere ressource libre
            sol.matrixTask[numMatrice][numTask] = t;
            numTask = (numTask + 1) % instance.numJobs; // colonne suivante dans la matrice
            if (numTask == 0)
                numMatrice++; // ligne suivante dans la matrice si on est revenu a la colonne 0

            //mettre a jour les taches realisables
            realise.add(t);
            feasibleTasks = feasibleTasks(realise, instance);
        }
        System.out.println("Greedy solver, resource order : " + sol);
        Schedule best = sol.toSchedule();
        return new Result(instance,best,Result.ExitCause.Timeout);
    }

    private ArrayList<Task> feasibleTasks(ArrayList<Task> realise, Instance instance){
        ArrayList<Task> feasibleTasks = new ArrayList<>();
        //parcourt de toutes les taches
        for(int t=0; t < instance.numTasks; t++){
            for(int j=0; j < instance.numJobs; j++) {
                if (!realise.contains(new Task(j,t))){ // si la tache n est pas deja realisee
                    if(t == 0 || realise.contains(new Task(j, t - 1))){
                        feasibleTasks.add(new Task(j,t));
                    }
                }
            }
        }
        return feasibleTasks;
    }
}
