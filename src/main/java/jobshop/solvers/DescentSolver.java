package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import java.util.List;

public class DescentSolver implements Solver {
    @Override

    public Result solve(Instance instance, long deadline) {
        //Init
        Schedule bestSolution = new GreedySolver(GreedySolver.Priority.SPT).solve(instance,deadline).schedule;

        //Boucle
        while (deadline - System.currentTimeMillis() > 1) {
            List<Voisinage.Block> blocks = Voisinage.blocksOfCriticalPath(new ResourceOrder(bestSolution));
            ResourceOrder bestVoisin = null;
            //recherche du meilleur voisin
            for (Voisinage.Block b : blocks) {
                List<Voisinage.Swap> voisins = Voisinage.neighbors(b);
                for (Voisinage.Swap swap : voisins) {
                    // voisin de la meilleure solution
                    ResourceOrder voisin = new ResourceOrder(bestSolution);
                    swap.applyOn(voisin);
                    Schedule voisinSched = voisin.toSchedule();
                    // si l'objectif est meilleur
                    if ((bestVoisin == null) || (voisinSched != null && obj(voisinSched) < obj(bestVoisin))) {
                        bestVoisin = voisin.copy();
                    }
                }
            }
            // fin de parcours des voisins, on a le meilleur
            if (obj(bestVoisin) < obj(bestSolution)) {
                bestSolution = bestVoisin.toSchedule();
            }else {
                return new Result(instance, bestSolution, Result.ExitCause.Timeout);
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