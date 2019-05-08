package BackEnd;

import IRClass.CFGNode;
import IRClass.FuncFrame;
import IRClass.Quad;
import OprandClass.AddrOprand;
import OprandClass.Oprand;
import OprandClass.RegOprand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class ActAnalysiser {

    public ActAnalysiser() { }

    public ArrayList <HashSet <Oprand> > getLiveOut(FuncFrame curfunc) {
        ArrayList <CFGNode> blocks = curfunc.getCfgList();

        ArrayList <HashSet <Oprand> > liveOut = new ArrayList<>();
        ArrayList <HashSet <Oprand> > UEVar = new ArrayList<>();
        ArrayList <HashSet <Oprand> > VarKill = new ArrayList<>();

        for (int i = 0 ; i < blocks.size() ; ++ i) {
            liveOut.add(new HashSet <Oprand>());

            HashSet <Oprand> curUEVar = new HashSet<>();
            HashSet <Oprand> curVarKill = new HashSet<>();
            UEVar.add(curUEVar);
            VarKill.add(curVarKill);

            for (Quad q = blocks.get(i).head ; q != null ; q = q.nxt) {
                HashSet <Oprand> used = q.getUsed();
                for (Oprand p : used) {
                    if (!curVarKill.contains(p)) {
                        curUEVar.add(p);
                    }
                }
                curVarKill.addAll(q.getDefined());
            }
        }

        while (true) {
            boolean changed = false;
            for (int i = blocks.size() - 1 ; i >= 0 ; -- i) {
                HashSet <Oprand> tmp = new HashSet<>();
                ArrayList <CFGNode> sucs = blocks.get(i).getTo();
                int oldSiz = liveOut.get(i).size();
                for (int j = 0 ; j < sucs.size() ; ++ j) {
                    int curSucIdx = sucs.get(j).getIdx();
                    tmp.addAll(liveOut.get(curSucIdx));
                    tmp.removeAll(VarKill.get(curSucIdx));
                    tmp.addAll(UEVar.get(curSucIdx));
                    liveOut.get(i).addAll(tmp);
                }
                if (oldSiz != liveOut.get(i).size()) {
                    changed = true;
                }
            }

            if (!changed) break;
        }

        return liveOut;
    }

    public Graph getConflictGraph(FuncFrame curfunc) {
        Graph g = new Graph();
        ArrayList <HashSet <Oprand> > liveOut = getLiveOut(curfunc);
        ArrayList <CFGNode> blocks = curfunc.getCfgList();

        for (int i = 0 ; i < blocks.size() ; ++ i) {
            for (Quad q = blocks.get(i).head ; q != null ; q = q.nxt) {
                g.addRegs(q.getUsed());
                g.addRegs(q.getDefined());
            }
        }

        for (int i = 0 ; i < blocks.size() ; ++ i) {
            HashSet <Oprand> liveNow = new HashSet<>(liveOut.get(i));
            for (Quad q = blocks.get(i).tail ; q != null ; q = q.pre) {
                for (Oprand reg1 : q.getDefined()) {
                    for (Oprand reg2 : liveNow) {
                        g.addEdge(reg1, reg2);
                    }
                }
                liveNow.removeAll(q.getDefined());
                liveNow.addAll(q.getUsed());
            }
        }

        return g;
    }
}
