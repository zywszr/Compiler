package BackEnd;

import IRClass.*;
import OprandClass.ImmOprand;
import OprandClass.Oprand;
import OprandClass.StackSlot;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import static BackEnd.RegisterSet.*;
import static IRClass.Inst.*;

public class CodeGen {
    LineIR lineIR;

    public CodeGen(LineIR _lineIR) {
        lineIR = _lineIR;
    }

    public void work() {
        for (FuncFrame func : lineIR.getFuncs()) {
            processFunc(func);
        }
    }

    void processFunc(FuncFrame curfunc) {
        LinkedList <Oprand> parameters = curfunc.parameters;
        if (parameters.size() > 6) {
            for (int i = 6 ; i < parameters.size() ; ++ i) {
                curfunc.params.add((StackSlot) parameters.get(i).memPos);
            }
        }
        HashSet <StackSlot> slotsSet = new HashSet<>();
        for (CFGNode block : curfunc.getCfgList()) {
            for (Quad q = block.head; q != null ; q = q.nxt) {
                if (q instanceof FuncQuad && q.op.equals(CALL)) {
                    long val = ((ImmOprand) q.getR1()).getVal();
                    if (val > 6) {
                        q.append(new ArthQuad(ADD, rsp, new ImmOprand((val - 6) * 8)));
                    }
                }
                for (StackSlot s : q.getStackSlots()) {
                    if (!curfunc.params.contains(s)) {
                        slotsSet.add(s);
                    }
                }
            }
        }
        curfunc.temps.addAll(slotsSet);
        for (int i = 0 ; i < curfunc.params.size() ; ++ i) {
            StackSlot s = curfunc.params.get(i);
            s.setBase(rbp);
            s.setDisp(new ImmOprand((long)(16 + 8 * i)));
        }
        for (int i = 0 ; i < curfunc.temps.size() ; ++ i) {
            StackSlot s = curfunc.temps.get(i);
            s.setBase(rbp);
            s.setDisp(new ImmOprand((long)(-8 - 8 * i)));
        }

        Quad head = curfunc.getStart().head;
        if (head == null) {
            curfunc.getStart().insertQuad(new PushQuad(rbp));
            head = curfunc.getStart().head;
            head.append(new ArthQuad(MOV, rbp, rsp));
            head = head.nxt;
            head.append(new ArthQuad(SUB, rsp, new ImmOprand((long) curfunc.getFuncSize())));
            head = head.nxt;
        } else {
            head.prepend(new PushQuad(rbp));
            head.prepend(new ArthQuad(MOV, rbp, rsp));
            head.prepend(new ArthQuad(SUB, rsp, new ImmOprand((long) curfunc.getFuncSize())));
            head = head.pre;
        }


        HashSet <Oprand> callees = new HashSet<>(curfunc.phyRegs);
        callees.retainAll(calleeSave);
        for (Oprand reg : callees) {
            head.append(new PushQuad(reg));
        }
        CFGNode end = curfunc.getEnd();
        if (end == null) {
            System.out.println(curfunc.getName());
        }
        for (Oprand reg : callees) {
            end.insertQuad(new PopQuad(reg));
        }
        end.insertQuad(new ArthQuad(MOV, rsp, rbp));
        end.insertQuad(new PopQuad(rbp));
        end.insertQuad(new FuncQuad(RET, null));
    }

    ArrayList <String> globals = new ArrayList<>();
    ArrayList <String> externs = new ArrayList<>();
    ArrayList <String> codes = new ArrayList<>();

    void initPrint() {
        globals.addAll(lineIR.getGlobal());
        globals.add("String_substring");
        globals.add("String_parseInt");
        globals.add("String_ord");
        globals.add("String_strcpy");
        globals.add("String_strcat");
        globals.add("String_length"); // may be the same
        globals.add("print");
        globals.add("println");
        globals.add("getString");
        globals.add("getInt");
        globals.add("toString");

        externs.add("strcmp");
        externs.add("__sprintf_chk");
        externs.add("_IO_getc");
        externs.add("stdin");
        externs.add("puts");
        externs.add("scanf");
        externs.add("_IO_putc");
        externs.add("stdout");
        externs.add("__stack_chk_fail");
        externs.add("sscanf");
        externs.add("memcpy");
        externs.add("malloc");
    }

    public void print() {
        initPrint();
        codes.add("default rel");
        codes.add("");
        for (int i = 0; i < globals.size(); ++i) {
            codes.add("global " + globals.get(i));
        }
        codes.add("");
        for (int i = 0; i < externs.size(); ++i) {
            codes.add("extern " + externs.get(i));
        }

        codes.add("");
        codes.add("SECTION .text");
        codes.add(PreCode.text);

        for (int i = 0 ; i < codes.size() ; ++ i) {
            System.out.println(codes.get(i));
        }

        codes.clear();

        lineIR.printCode();

        codes.add("\n" + "\n" + "SECTION .data    align=8");
        codes.add("");
        codes.add("\n" + "\n" + "SECTION .bss     align=8");

        for (String globalVar : lineIR.getGlobalVar()) {
            codes.add(globalVar + ":");
            codes.add(String.format("%-8s resq %d", " ", 1));
        }

        codes.add("\n" + "SECTION .rodata");
        for (Pair <String, String> str : lineIR.getRoData()) {
            codes.add(str.getKey() + ": ");
            codes.add(String.format("%-8s db %s", " ", str.getValue()));
        }

        codes.add(PreCode.roData);
        codes.add("");

        for (int i = 0 ; i < codes.size() ; ++ i) {
            System.out.println(codes.get(i));
        }

        codes.clear();
    }
}
