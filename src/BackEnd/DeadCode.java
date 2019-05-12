package BackEnd;

import IRClass.CFGNode;
import IRClass.FuncFrame;
import IRClass.LineIR;
import IRClass.Quad;
import OprandClass.Oprand;
import OprandClass.RegOprand;

import static IRClass.Inst.*;

public class DeadCode {
    LineIR lineIR;

    public DeadCode(LineIR _lineIR) {
        lineIR = _lineIR;
    }

    public void work_before_allocate() {

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
