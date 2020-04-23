package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.HashMap;
import java.util.List;

public class TabooSolver implements Solver {
    private int maxIter;
    private int dureeTaboo;

    public TabooSolver(int iter, int taboo){
        this.maxIter = iter;
        this.dureeTaboo = taboo;
    }

    /**
     * solutionsTaboo
     * Consider the solution in ResourceOrder representation
     * machine 1 : (0,1) (1,2) (2,2)
     * machine 2 : (0,2) (2,1) (1,1)
     *          m1,t1   m1,t2   m1,t3   m2,t1   m2,t2   m2,t3
     * m1,t1      0       0       0       0       0       0
     * m1,t2      0       0       0       0       0       0
     * m1,t3      0       0       0       0       0       0
     * m2,t1      0       0       0       0       0       0
     * m2,t2      0       0       0       0       0       0
     * m2,t3      0       0       0       0       0       0
     */

    @Override
    public Result solve(Instance instance, long deadline) {
        //Init
        Schedule bestSolution = new GreedySolver(GreedySolver.Priority.SPT).solve(instance,deadline).schedule;
        int k = 0;
        long timerStart = System.currentTimeMillis();
        //matrice de taches, ligne + colonne = permutation
        int [][] solutionsTaboo = new int [instance.numJobs * instance.numTasks][instance.numJobs * instance.numTasks];

        List<Voisinage.Block> blocks = Voisinage.blocksOfCriticalPath(new ResourceOrder(bestSolution));
        ResourceOrder bestVoisin = null;
        Voisinage.Swap bestSwap = null; // enregistre le swap du meilleur voisin pour ajouter son opposé dans taboo
        //Boucle
        //recherche du meilleur voisin
        for(Voisinage.Block b : blocks){
            List<Voisinage.Swap> voisins = Voisinage.neighbors(b);
            k++;
            for(Voisinage.Swap swap : voisins){
                // voisin de la meilleure solution
                ResourceOrder voisin = new ResourceOrder(bestSolution);
                // is not taboo?
                if(solutionsTaboo[swap.t1 + instance.numTasks * swap.machine][swap.t2 + instance.numTasks * swap.machine] < k){
                    swap.applyOn(voisin);
                    Schedule voisinSched = voisin.toSchedule();
                    // si l'objectif est meilleur
                    if(bestVoisin == null){
                        bestVoisin = voisin.copy();
                        bestSwap = new Voisinage.Swap(swap.machine,swap.t1,swap.t2);
                    }else{
                        if(voisinSched != null && obj(voisinSched) < obj(bestVoisin)){
                            bestVoisin = voisin.copy();
                            bestSwap = new Voisinage.Swap(swap.machine,swap.t1,swap.t2);
                        }
                    }
                }
            }
            // fin de parcours des voisins, on a le meilleur
            //ajout dans taboo
            solutionsTaboo[bestSwap.t2 + instance.numTasks * bestSwap.machine][bestSwap.t1 + instance.numTasks * bestSwap.machine] = k + dureeTaboo;
            //regarde si ameliorant
            if(obj(bestVoisin) < obj(bestSolution)){
                bestSolution = bestVoisin.toSchedule();
                bestVoisin = null;
            }
            // max iter depassee ou timeout
            if((k >= maxIter) || (System.currentTimeMillis() > (timerStart + deadline))){
                break;
            }
        }
        return new Result(instance, bestSolution, Result.ExitCause.Timeout);
    }

    private int obj(ResourceOrder order){
        return order.toSchedule().makespan();
    }

    private int obj(Schedule sched){
        return sched.makespan();
    }

    private boolean isTaboo(Voisinage.Swap swap, int iter, int [][] solutionsTaboo){
        if(solutionsTaboo[swap.t1][swap.t2] < iter){
            return true;
        }else{
            return false;
        }
    }
}
