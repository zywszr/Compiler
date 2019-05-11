package BackEnd;

import IRClass.*;
import OprandClass.*;

import java.util.ArrayList;
import java.util.HashSet;

import static BackEnd.RegisterSet.*;
import static IRClass.Inst.*;

public class IRCorrector {
    int tmpVarIdx;
    LineIR lineIR;
    FuncFrame curfunc;
    // 3 to 2               *********
    // mul div mod          *********
    // mul opti             *********
    // params               *********
    // return               *********
    // mov mem to mem       *********
    // sal sar tmp or imm   *********
    // get defined and used *********
    // push pop             *********

    RegOprand newTempVar(boolean isAddr) {
        tmpVarIdx += 1;
        return getReg((isAddr ? "A" : "V") + "_" + Integer.toString(tmpVarIdx), true, 0);
    }

    public int getTmpVarIdx() {
        return tmpVarIdx;
    }

    public IRCorrector(LineIR _lineIR, int _tmpVarIdx) {
        lineIR = _lineIR;
        tmpVarIdx = _tmpVarIdx;
   }

    public void work() {
        ArrayList <FuncFrame> funcs = lineIR.getFuncs();
        for (int i = 0 ; i < funcs.size() ; ++ i) {
            curfunc = funcs.get(i);
            processFunc();
        }
    }

    boolean checkKsm(long x) {
        if (x <= 0) return false;
        while (x != 1) {
            if ((x & 1) > 0) return false;
            x >>= 1;
        }
        return true;
    }

    long valToLog(long x) {
        long ret = 0;
        while (x != 1) {
            ++ ret;
            x >>= 1;
        }
        return ret;
    }

    public void solveArth(ArthQuad q) {
        Oprand tmp = newTempVar(false), newTmp = null;
        while (true) {
            boolean isChanged = false;
            switch (q.op) {
                case "mul":
                    if (q.getR2() instanceof ImmOprand && checkKsm(((ImmOprand) q.getR2()).getVal())) {
                        long k = valToLog(((ImmOprand) q.getR2()).getVal());
                        q.op = SAL;
                        q.setR2(new ImmOprand(k));
                        isChanged = true;
                    } else {
                        q.prepend(new ArthQuad(MOV, rax, q.getR1()));
                        q.append(new ArthQuad(MOV, q.getRt(), rax));
                        q.op = IMUL;
                        q.setR1(q.getR2());
                        q.setR2(null);
                        q.setRt(null); // remember to test
                        if (!(q.getR1() instanceof RegOprand)) {
                            Oprand newR1 = newTempVar(false);
                            q.prepend(new ArthQuad(MOV, newR1, q.getR1()));
                            q.setR1(newR1);
                        }
                    }
                    break;
                case "div":
                case "mod":
                    if (q.getR2() instanceof ImmOprand && checkKsm(((ImmOprand) q.getR2()).getVal())) {
                        long k = valToLog(((ImmOprand) q.getR2()).getVal());
                        if (q.op.equals(DIV)) {
                            q.op = SAR;
                            q.setR2(new ImmOprand(k));
                            isChanged = true;
                        } else {
                            q.op = AND;
                            q.setR2(new ImmOprand((long)((1 << k) - 1)));
                            isChanged = true;
                        }
                    } else {
                        q.prepend(new ArthQuad(MOV, rax, q.getR1()));
                        q.prepend(new ArthQuad(CDQ));
                        if (q.op.equals(DIV)) {
                            q.append(new ArthQuad(MOV, q.getRt(), rax));
                        } else {
                            q.append(new ArthQuad(MOV, q.getRt(), rdx));
                        }
                        q.op = IDIV;
                        q.setR1(q.getR2());
                        q.setR2(null);
                        q.setRt(null); // remember to test
                        if (!(q.getR1() instanceof RegOprand)) {
                            Oprand newR1 = newTempVar(false);
                            q.prepend(new ArthQuad(MOV, newR1, q.getR1()));
                            q.setR1(newR1);
                        }
                    }
                    break;
                case "sal":
                case "sar":
                    if (q.getRt() instanceof MemOprand) {
                        newTmp = newTempVar(false);
                    } else {
                        newTmp  = q.getRt();
                    }
                    q.prepend(new ArthQuad(MOV, newTmp, q.getR1()));
                    q.prepend(new ArthQuad(MOV, rcx, q.getR2()));
                    q.setR1(rcx);
                    q.setR2(null);
                    if (q.getRt() instanceof MemOprand) {
                        q.append(new ArthQuad(MOV, q.getRt(), newTmp));
                        q.setRt(newTmp);
                    }

                    break;
                case "add":
                case "sub":
                case "and":
                case "or":
                case "xor":
                case "not":
                case "neg":
                    if (q.getRt() instanceof MemOprand) {
                        newTmp = newTempVar(false);
                    } else {
                        newTmp = q.getRt();
                    }
                    q.prepend(new ArthQuad(MOV, newTmp, q.getR1()));

                    if (q.op.equals(NOT) || q.op.equals(NEG)) {
                        q.setR1(null);
                    } else {
                        q.setR1(q.getR2());
                        q.setR2(null);
                    }

                    if (q.getRt() instanceof MemOprand) {
                        q.append(new ArthQuad(MOV, q.getRt(), newTmp));
                        q.setRt(newTmp);
                    }
                    break;
                default:
                    break;
            }
            if (!isChanged) break;
        }
    }

    boolean checkCall(String name) {
        for (FuncFrame func : lineIR.getFuncs()) {
            if ((!func.getName().equals("___init")) && func.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    void solveFunc(FuncQuad q) {
        switch (q.op) {
            case "param":
                long idx = ((ImmOprand) q.getR2()).getVal();
                if (idx < 6) {
                    q.replace(new ArthQuad(MOV, args.get((int) idx), q.getR1()));
                } else {
                    q.replace(new PushQuad(q.getR1()));
                }
                break;
            case "call":
                if (checkCall(q.funcName)) {
                    HashSet <Oprand> set = new HashSet<>(curfunc.globalVarUsed);
                    // set.retainAll(lineIR.stringToFunc.get(q.funcName).callVarDefined);
                    for (Oprand var : set) {
                        GlobalMemOprand mem = new GlobalMemOprand(var);
                        var.setMemPos(mem);
                        q.append(new ArthQuad(MOV, var, mem));
                    }

                    set = new HashSet<>(curfunc.globalVarDefined);
                    // set.addAll(lineIR.stringToFunc.get(q.funcName).callVarUsed);
                    // set.retainAll(curfunc.globalVarDefined);

                    for (Oprand var : set) {
                        // System.out.println(((RegOprand) var).getRegName());

                        GlobalMemOprand mem = new GlobalMemOprand(var);
                        var.setMemPos(mem);
                        q.prepend(new ArthQuad(MOV, mem, var));
                    }

                }
                if (q.getRt() != null) {
                    q.append(new ArthQuad(MOV, q.getRt(), rax));
                    q.setRt(rax);
                }
                break;
            case "ret":
                q.replace(new ArthQuad(MOV, rax, q.getR1()));
                break;
        }
    }

    void solveMove(ArthQuad q) {     // remember to test
        if ((q.getRt() instanceof MemOprand) && (q.getR1() instanceof MemOprand)) {
            RegOprand tmp = newTempVar(false);
            q.prepend(new ArthQuad(MOV, tmp, q.getR1()));
            q.setR1(tmp);
        } else if((q.getRt() instanceof RegOprand) && (q.getR1() instanceof RegOprand)) {
            if (((RegOprand) q.getRt()).getRegName().equals(((RegOprand) q.getR1()).getRegName())) {
                q.remove();
            }
        }
    }

    void solveComp(CompQuad q) {
        if (q.getR1() instanceof ImmOprand) {
            Oprand tmp = q.getR2();
            q.setR2(q.getR1());
            q.setR1(tmp);
            switch (q.nxt.op) {
                case "jl":
                    q.nxt.op = "jg";
                    break;
                case "jle":
                    q.nxt.op = "jge";
                    break;
                case "jg":
                    q.nxt.op = "jl";
                    break;
                case "jge":
                    q.nxt.op = "jle";
                    break;
            }
        }
        if (q.getR1() instanceof MemOprand) {
            Oprand tmp = newTempVar(false);
            q.prepend(new ArthQuad(MOV, tmp, q.getR1()));
            q.setR1(tmp);
        }
        /*
        if (q.getR2() instanceof MemOprand) {
            Oprand tmp = newTempVar(false);
            q.prepend(new ArthQuad(MOV, tmp, q.getR2()));
            q.setR2(tmp);
        }
        */
    }

    void processFunc() {
        ArrayList <CFGNode> blocks = curfunc.getCfgList();
        for (int i = 0 ; i < blocks.size() ; ++ i) {
            CFGNode curblock = blocks.get(i);
            for (Quad q = curblock.head ; q != null ; q = q.nxt) {
                if (q instanceof ArthQuad) {
                    solveArth((ArthQuad) q);
                } else if (q instanceof FuncQuad) {
                    solveFunc((FuncQuad) q);
                } else if (q instanceof CompQuad) {
                    solveComp((CompQuad) q);
                } else if (q instanceof JumpQuad) {
                    if ((!q.op.equals("jmp")) && (((JumpQuad) q).getLabel2() != null)) {
                        q.append(new JumpQuad(JMP, ((JumpQuad) q).getLabel2()));
                        ((JumpQuad) q).setLabel2(null);
                    }
                }
            }

            for (Quad q = curblock.head ; q != null ; q = q.nxt) {
                if (q instanceof ArthQuad) {
                    if (q.op.equals(MOV)) {
                        solveMove((ArthQuad) q);
                    } else if (q.op.equals(ADD)) {
                        if ((q.getRt() instanceof RegOprand) && (q.getR1() instanceof ImmOprand)) {
                            if (((ImmOprand) q.getR1()).getVal() == 1L) {
                                q.op = INC;
                                q.setR1(null);
                            }
                        }
                    }
                }
            }
        }
    }
}
