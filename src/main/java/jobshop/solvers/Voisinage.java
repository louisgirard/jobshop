package jobshop.solvers;

import jobshop.Schedule;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class Voisinage {

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
    
    /** Returns a list of all blocks of the critical path. */
    static List<Block> blocksOfCriticalPath(ResourceOrder order) {
        ArrayList<Block> blocks = new ArrayList<Block>();
        Schedule sched = order.toSchedule();
        List<Task> criticalPath = sched.criticalPath();

        Task t = criticalPath.get(0);
        int numMachine = order.instance.machine(t.job,t.task);
        Task taskDebut = new Task(t.job,t.task);
        int nbTask = 1;

        for (int i = 1; i < criticalPath.size(); i++){
            t = criticalPath.get(i);
            //tache consecutive utilisant la meme ressource
            if(numMachine == order.instance.machine(t.job,t.task)){
                nbTask++;
            }else{
                // pas la meme ressource mais il y avait deja au moins 2 taches consecutives avant
                if(nbTask >= 2){
                    int debut = debut(taskDebut,numMachine,order);
                    blocks.add(new Block(numMachine,debut,debut + nbTask - 1));
                }
                nbTask = 1;
                taskDebut = new Task(t.job,t.task);
                numMachine = order.instance.machine(t.job,t.task);
            }
        }
        // on a fini le parcours du chemin critique et il y avait au moins 2 taches consecutives
        if(nbTask >= 2){
            int debut = debut(taskDebut,numMachine,order);
            blocks.add(new Block(numMachine,debut,debut + nbTask - 1));
        }
        return blocks;
    }

    private static int debut(Task t, int m, ResourceOrder order){
        for(int i = 0; i < order.matrixTask[m].length; i++){
            if(t.equals(order.matrixTask[m][i])){
                return i;
            }
        }
        return -1;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    static List<Swap> neighbors(Block block) {
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
