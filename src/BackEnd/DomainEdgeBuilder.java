package BackEnd;

import IRClass.CFGNode;
import IRClass.FuncFrame;

import java.util.ArrayList;

public class DomainEdgeBuilder {
    FuncFrame curfunc;

    public DomainEdgeBuilder(FuncFrame func) {
        curfunc = func;
    }

    public ArrayList <CFGNode> getSame(ArrayList <CFGNode> a, ArrayList <CFGNode> b) {
        ArrayList <CFGNode> ret = new ArrayList<>();
        for (CFGNode block : a) {
            if (b.contains(block)) {
                ret.add(block);
            }
        }
        return ret;
    }

    public void buildEdge() {
        ArrayList <ArrayList<CFGNode> > pres = new ArrayList<>();
        ArrayList <CFGNode> blocks = curfunc.getCfgList();
        for (int i = 0 ; i < blocks.size() ; ++ i) {
            pres.add(new ArrayList<CFGNode>());
            CFGNode label = blocks.get(i);
            if (label.hasFrom()) {
                pres.get(i).addAll(blocks);
            } else {
                pres.get(i).add(label);
            }
        }
        while (true) {
            boolean flag = false;
            for (int i = 0 ; i < blocks.size() ; ++ i) {
                CFGNode curBlock = blocks.get(i);
                if (!curBlock.hasFrom()) continue;
                ArrayList <CFGNode> froms = curBlock.getFrom();
                ArrayList <CFGNode> tmp = new ArrayList<>(pres.get(froms.get(0).getIdx()));
                for (int j = 1 ; j < froms.size() ; ++ j) {
                    tmp = getSame(tmp, pres.get(froms.get(j).getIdx()));
                }
                tmp.add(curBlock);
                if (!tmp.equals(pres.get(i))) {
                    pres.set(i, tmp);
                    flag = true;
                }
            }
            if (!flag) break;
        }

        ArrayList <CFGNode> lca = new ArrayList<>();
        ArrayList <ArrayList <CFGNode>> commonSuc = new ArrayList<>();
        ArrayList <ArrayList <CFGNode>> branchSuc = new ArrayList<>();
        for (int i = 0 ; i < blocks.size() ; ++ i) {
            commonSuc.add(new ArrayList<CFGNode>());
            branchSuc.add(new ArrayList<CFGNode>());
            CFGNode curBlock = blocks.get(i);
            if (!curBlock.hasFrom()) {
                lca.add(curBlock);
            } else {
                lca.add(pres.get(i).get(pres.get(i).size() - 2));
            }
        }

        for (int i = 0 ; i < blocks.size() ; ++ i) {
            if (lca.get(i) != null) {
                commonSuc.get(lca.get(i).getIdx()).add(blocks.get(i));
            }
        }

        for (int i = 0 ; i < blocks.size() ; ++ i) {
            CFGNode curBlock = blocks.get(i);
            ArrayList <CFGNode> froms = curBlock.getFrom();
            if (froms.size() <= 1) continue;
            for (CFGNode from : froms) {
                while (from != lca.get(i)) {
                    branchSuc.get(from.getIdx()).add(curBlock);
                    from = lca.get(from.getIdx());
                }
            }
        }

        for (int i = 0 ; i < blocks.size() ; ++ i) {
            blocks.get(i).setCommonSuc(commonSuc.get(i));
            blocks.get(i).setBranchSuc(branchSuc.get(i));
        }
    }
}
