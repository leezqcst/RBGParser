package parser.sampling;

import java.util.*;
import gnu.trove.list.array.*;
import parser.*;
import utils.Utils;

public class RandomWalkSampler {
	
	Random r;
	
	public RandomWalkSampler(int seed) {
		r = new Random(seed);
	}
	
	public RandomWalkSampler() {
		r = new Random(1/*System.currentTimeMillis()*/);
	}
	
	public RandomWalkSampler(Random r) {
		this.r = r;
	}
	
	
    public DependencyInstance randomWalkSampling(DependencyInstance inst,
    		LocalFeatureData lfd, boolean addLoss) {
    	int loopCount = 0;
    	int len = inst.length;
    	
		DependencyInstance predInst = new DependencyInstance(inst);
		predInst.heads = new int[len];
		predInst.deplbids = new int[len];
    	
    	boolean[] inTree = new boolean[len];
    	inTree[0] = true;
    	for (int i = 0; i < len; i++) {
    		predInst.heads[i] = -1;
    	}
    	
    	for (int i = 1; i < len; i++) {
    		int curr = i;
    		while (!inTree[curr]) {
    			// sample new head
    			
    			TDoubleArrayList score = new TDoubleArrayList();
    			TIntArrayList depList = new TIntArrayList();

    			for (int candH = 0; candH < len; candH++) {
    				if (lfd.isPruned(candH, curr))
    					continue;
    				depList.add(candH);
    				double s = lfd.getArcScore(candH, curr);
    				if (addLoss && inst.heads[curr] != candH) {
    					// cost augmented
    					s += 1.0;
    				}
    				score.add(s);
    			}

    			int sample = samplePoint(score, r);
    			predInst.heads[curr] = depList.get(sample);
    			curr = predInst.heads[curr];
    			
    			if (predInst.heads[curr] != -1 && !inTree[curr]) {
    				cycleErase(predInst.heads, curr);
    				++loopCount;
    				if (loopCount % 10000000 == 0)
    					System.out.println("\tRndWalk Loop " + loopCount);
    			}
    		}
    		curr = i;
    		while (!inTree[curr]) {
    			inTree[curr] = true;
    			curr = predInst.heads[curr]; 
    		}
    	}
    	
    	return predInst;
    }
    
    private void cycleErase(int[] dep, int i) {
    	while (dep[i] != -1) {
    		int next = dep[i];
    		dep[i] = -1;
    		i = next;
    	}
    }
    
    private int samplePoint(TDoubleArrayList score, Random r) {
    	double sumScore = Double.NEGATIVE_INFINITY;
    	int N = score.size();
    	for (int i = 0; i < N; i++) {
    		sumScore = Utils.logSumExp(sumScore, score.get(i));
    	}
    	double logp = Math.log(r.nextDouble() + 1e-30);
    	double cur = Double.NEGATIVE_INFINITY;
    	int ret = 0;
    	for (; ret < N; ret++) {
    		cur = Utils.logSumExp(cur, score.get(ret));
    		if (logp + sumScore < cur)
    			break;
    	}
    	return ret;
    }
}
