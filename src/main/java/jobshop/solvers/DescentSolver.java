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
        ResourceOrder res = new ResourceOrder(instance);
        res.matrixTask[0][0] = new Task(0,0);
        res.matrixTask[0][1] = new Task(1,1);
        res.matrixTask[1][0] = new Task(1,0);
        res.matrixTask[1][1] = new Task(0,1);
        res.matrixTask[2][0] = new Task(0,2);
        res.matrixTask[2][1] = new Task(1,2);
        List<Block> blocks = blocksOfCriticalPath(res);
        for(Block b : blocks){
            System.out.println("Block, machine : " + b.machine + ", first task : " + b.firstTask + ", last task : " + b.lastTask);
            neighbors(b).get(0).applyOn(res);
        }
        return new Result(instance, res.toSchedule(), Result.ExitCause.Timeout);
    }

    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order) {
        ArrayList<Block> blocks = new ArrayList<Block>();
        Schedule sched = order.toSchedule();
        List<Task> criticalPath = sched.criticalPath();

        int debut = -1;
        int nbTask = 0;

        //parcourt du chemin critique
        for(int m = 0; m < order.instance.numMachines; m++){
            for(int t = 0; t < order.instance.numJobs; t++){
                if (criticalPath.contains(order.matrixTask[m][t])){
                    if (nbTask == 0){
                        debut = t;
                    }
                    nbTask++;
                }else{
                    //si on tombe sur une tache qui n'est pas dans le critical path mais qu'il y avait plus de
                    // 2 taches avant qui y etaient alors on a un block
                    if(nbTask >= 2){
                        blocks.add(new Block(m,debut,t-1));
                    }
                    debut = -1;
                    nbTask = 0;
                }
            }
            //si la derniere tache de la ligne pouvait faire un bloc
            if(nbTask >= 2){
                blocks.add(new Block(m,debut,order.instance.numJobs - 1));
            }
            debut = -1;
            nbTask = 0;
        }
        return blocks;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        ArrayList<Swap> voisins = new ArrayList<Swap>();
        //si uniquement 2 taches
        if((block.lastTask - block.firstTask) == 1){
            voisins.add(new Swap(block.machine, block.firstTask, block.lastTask));
        }else
        {
            voisins.add(new Swap(block.machine, block.firstTask, block.firstTask + 1));
            voisins.add(new Swap(block.machine, block.lastTask - 1, block.lastTask));
        }
        return voisins;
    }

}