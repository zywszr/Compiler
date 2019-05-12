package BackEnd;

import IRClass.*;
import OprandClass.GlobalMemOprand;
import OprandClass.ImmOprand;
import OprandClass.Oprand;
import OprandClass.RegOprand;

import java.util.HashMap;
import java.util.HashSet;

import static IRClass.Inst.*;

public class GlobalSolver {
    LineIR lineIR;
    HashMap<FuncFrame, HashSet <FuncFrame> > calls;

    public GlobalSolver(LineIR _lineIR) {
        lineIR = _lineIR;
        calls = new HashMap<>();
    }

    boolean checkCall(String name) {
        for (FuncFrame func : lineIR.getFuncs()) {
            if ((!func.getName().equals("___init")) && func.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    void printCheck() {
        for (FuncFrame func : lineIR.getFuncs()) {
            System.out.println(func.getName() + "  used:");
            if (func.getName().equals("___init")) continue;
            for (Oprand reg : func.callVarUsed) {
                System.out.print(((RegOprand) reg).getRegName() + ", ");
            }
            System.out.println();
        }
    }

    public void work() {

        for (FuncFrame func : lineIR.getFuncs()) {
            CFGNode start = func.getStart(), end = func.getEnd();
            for (Oprand var : func.globalVarUsed) {
                // System.out.println(((RegOprand) var).getRegName() + "--------------------------");
                GlobalMemOprand mem = new GlobalMemOprand(var);
                var.setMemPos(mem);
                start.prepend(new ArthQuad(MOV, var, mem));
            }
            if (func.getName().equals("main")) {
                start.prepend(new FuncQuad(CALL, null, "___init", new ImmOprand(0L)));
            }
            for (Oprand var : func.globalVarDefined) {
                GlobalMemOprand mem = new GlobalMemOprand(var);
                var.setMemPos(mem);
                end.prepend(new ArthQuad(MOV, mem, var));
            }
            FuncQuad q = new FuncQuad(RET, null);
            q.isReaRet = true;
            end.append(q);
        }

        for (FuncFrame func : lineIR.getFuncs()) {
            HashSet <FuncFrame> curCall = new HashSet<>();
            calls.put(func, curCall);
            for (CFGNode block : func.getCfgList()) {
                for (Quad q = block.head ; q != null ; q = q.nxt) {
                    if (q.op.equals(CALL) && checkCall(((FuncQuad) q).funcName)) {
                        FuncFrame to = lineIR.stringToFunc.get(((FuncQuad) q).funcName);
                        curCall.add(to);
                    }
                }
            }
           /* System.out.println(func.getName() + "+++++++++++++++++++++++++++++++++");
            for (FuncFrame to : curCall) {
                System.out.print(to.getName() + " ");
            }
            System.out.println();*/
        }

        for (FuncFrame func : lineIR.getFuncs()) {
            if (func.getName().equals("___init")) continue;
            func.callVarUsed.addAll(func.globalVarUsed);
            func.callVarDefined.addAll(func.globalVarDefined);
        }

        while (true) {
            boolean changed = false;
            for (FuncFrame func : lineIR.getFuncs()) {
                if (func.getName().equals("___init")) continue;
                HashSet <FuncFrame> curCall = calls.get(func);
                int oldSize = func.callVarUsed.size();
                for (FuncFrame callFunc : curCall) {
                    func.callVarUsed.addAll(callFunc.callVarUsed);
                }
                if (func.callVarUsed.size() != oldSize) {
                    changed = true;
                }
            }

            if (!changed) break;
        }

        while (true) {
            boolean changed = false;
            for (FuncFrame func : lineIR.getFuncs()) {
                if (func.getName().equals("___init")) continue;
                HashSet <FuncFrame> curCall = calls.get(func);
                int oldSize = func.callVarDefined.size();
                for (FuncFrame callFunc : curCall) {
                    func.callVarDefined.addAll(callFunc.callVarDefined);
                }
                if (func.callVarDefined.size() != oldSize) {
                    changed = true;
                }
            }

            if (!changed) break;
        }
        // printCheck();
    }
}
