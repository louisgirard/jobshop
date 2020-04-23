package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import sun.jvm.hotspot.opto.Block;

import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swap with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task auxi = order.matrixTask[machine][t1];
            order.matrixTask[machine][t1] = order.matrixTask[machine][t2];
            order.matrixTask[machine][t2] = auxi;
        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
        //Init
        Schedule solution = new GreedySolver(GreedySolver.Priority.SPT).solve(instance,deadline).schedule;
        Schedule bestSolution = solution;
        List<Voisinage.Block> blocks = Voisinage.blocksOfCriticalPath(new ResourceOrder(bestSolution));
        ResourceOrder bestVoisin = null;

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