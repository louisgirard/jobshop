package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
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
        Schedule currentSolution = bestSolution;
        int k = 0;
        //matrice de taches, ligne + colonne = permutation
        int [][] solutionsTaboo = new int [instance.numJobs * instance.numTasks][instance.numJobs * instance.numTasks];

        while ((deadline - System.currentTimeMillis() > 1) && (k < maxIter)) {
            k++;
            List<Voisinage.Block> blocks = Voisinage.blocksOfCriticalPath(new ResourceOrder(currentSolution));
            ResourceOrder bestVoisin = null;
            Voisinage.Swap bestSwap = null; // enregistre le swap du meilleur voisin pour ajouter son opposé dans taboo
            //Boucle
            //recherche du meilleur voisin
            for (Voisinage.Block b : blocks) {
                List<Voisinage.Swap> voisins = Voisinage.neighbors(b);
                for (Voisinage.Swap swap : voisins) {
                    // voisin de la meilleure solution
                    ResourceOrder voisin = new ResourceOrder(currentSolution);
                    // is not taboo?
                    if (solutionsTaboo[swap.t1 + instance.numJobs * swap.machine][swap.t2 + instance.numJobs * swap.machine] < k) {
                        swap.applyOn(voisin);
                        Schedule voisinSched = voisin.toSchedule();
                        // si l'objectif est meilleur
                        if (bestVoisin == null || (voisinSched != null && obj(voisinSched) < obj(bestVoisin))) {
                            bestVoisin = voisin.copy();
                            bestSwap = new Voisinage.Swap(swap.machine, swap.t1, swap.t2);
                        }
                    }
                }
            }
            // fin de parcours des voisins, on a le meilleur
            //ajout dans taboo
            solutionsTaboo[bestSwap.t2 + instance.numJobs * bestSwap.machine][bestSwap.t1 + instance.numJobs * bestSwap.machine] = k + dureeTaboo;
            currentSolution = bestVoisin.toSchedule();
            //regarde si ameliorant
            if (obj(bestVoisin) < obj(bestSolution)) {
                bestSolution = bestVoisin.toSchedule();
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

}
