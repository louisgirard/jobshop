package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.List;

public class DescentSolver implements Solver {
    @Override
    public Result solve(Instance instance, long deadline) {
        //Init
        Schedule bestSolution = new GreedySolver(GreedySolver.Priority.SPT).solve(instance,deadline).schedule;
        List<Voisinage.Block> blocks = Voisinage.blocksOfCriticalPath(new ResourceOrder(bestSolution));
        ResourceOrder bestVoisin = null;
        long timerStart = System.currentTimeMillis();

        //Boucle
        //recherche du meilleur voisin
        for(Voisinage.Block b : blocks){
            List<Voisinage.Swap> voisins = Voisinage.neighbors(b);
            for(Voisinage.Swap swap : voisins){
                // voisin de la meilleure solution
                ResourceOrder voisin = new ResourceOrder(bestSolution);
                swap.applyOn(voisin);
                Schedule voisinSched = voisin.toSchedule();
                // si l'objectif est meilleur
                if(bestVoisin == null){
                    bestVoisin = voisin.copy();
                }else{
                    if(voisinSched != null && obj(voisinSched) < obj(bestVoisin)){
                        bestVoisin = voisin.copy();
                    }
                }
            }
            // fin de parcours des voisins, on a le meilleur
            if(obj(bestVoisin) < obj(bestSolution)){
                bestSolution = bestVoisin.toSchedule();
                bestVoisin = null;
            }else{
                break; // pas de solution ameliorante, on s'arrete
            }
            if(System.currentTimeMillis() > (timerStart + deadline)){
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

}