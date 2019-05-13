package BackEnd;

import IRClass.*;
import OprandClass.Oprand;
import OprandClass.RegOprand;

import java.util.ArrayList;
import java.util.HashSet;

import static IRClass.Inst.*;

public class DeadCode {
    LineIR lineIR;

    public DeadCode(LineIR _lineIR) {
        lineIR = _lineIR;
    }

    boolean checkRemove(Quad q) {
        if (q instanceof ArthQuad) return true;
        return false;
    }

    public void work_before_allocate() {
        ActAnalysiser analysiser = new ActAnalysiser();
        for (FuncFrame func : lineIR.getFuncs()) {
            ArrayList <HashSet <Oprand> > liveOut = analysiser.getLiveOut(func);
            ArrayList <CFGNode> blocks = func.getCfgList();
            for (int i = 0 ; i < blocks.size() ; ++ i) {
                HashSet <Oprand> liveNow = new HashSet<>(liveOut.get(i));
                CFGNode block = blocks.get(i);
                boolean isDead = true;
                for (Quad q = block.tail ; q != null ; q = q.pre) {
                    HashSet <Oprand> defined = q.getDefined();
                    if (defined.isEmpty()) {
                        isDead = false;
                    }
                    for (Oprand reg : defined) {
                        if (liveNow.contains(reg)) {
                            isDead = false;
                            break;
                        }
                    }
                    if (isDead && checkRemove(q)) {
                        q.remove();
                    } else {
                        HashSet <Oprand> used = q.getUsed();
                        liveNow.removeAll(defined);
                        liveNow.addAll(used);
                    }
                }
            }
        }
    }

    public void work_after_allocate() {
        for (FuncFrame func : lineIR.getFuncs()) {
            for (CFGNode block : func.getCfgList()) {
                for (Quad q = block.head ; q != null ; q = q.nxt) {
                    if (q.op.equals(MOV)) {
                        Oprand rt = q.getRt(), r1 = q.getR1();
                        if (rt instanceof RegOprand && r1 instanceof  RegOprand) {
                            if (((RegOprand) rt).getRegName().equals(((RegOprand) r1).getRegName())) {
                                q.remove();
                            }
                        }
                    }
                }
                for (Quad q = block.head ; q != null ; q = q.nxt) {
                    if (q.op.equals(MOV)) {
                        if (q.pre != null && q.pre.op.equals(MOV)) {
                            String rt1 = q.getRt().getCode(), r11 = q.getR1().getCode();
                            String rt2 = q.pre.getRt().getCode(), r12 = q.pre.getR1().getCode();
                            if (rt1.equals(r12) && r11.equals(rt2)) {
                                q.remove();
                            }
                        }
                    }
                }
            }
        }

    }

}
