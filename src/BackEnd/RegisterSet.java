package BackEnd;

import OprandClass.PhyRegOprand;
import OprandClass.RegOprand;

import java.util.ArrayList;
import java.util.HashMap;

public class RegisterSet {
    public static HashMap<String, RegOprand> virRegs;

    public static ArrayList <PhyRegOprand> callerSave;
    public static ArrayList <PhyRegOprand> calleeSave;
    public static ArrayList <PhyRegOprand> args;

    public static PhyRegOprand rax = new PhyRegOprand("rax");
    public static PhyRegOprand rcx = new PhyRegOprand("rcx");
    public static PhyRegOprand rdx = new PhyRegOprand("rdx");
    public static PhyRegOprand rbx = new PhyRegOprand("rbx");
    public static PhyRegOprand rsp = new PhyRegOprand("rsp");
    public static PhyRegOprand rbp = new PhyRegOprand("rbp");
    public static PhyRegOprand rsi = new PhyRegOprand("rsi");
    public static PhyRegOprand rdi = new PhyRegOprand("rdi");
    public static PhyRegOprand r8  = new PhyRegOprand("r8");
    public static PhyRegOprand r9  = new PhyRegOprand("r9");
    public static PhyRegOprand r10 = new PhyRegOprand("r10");
    public static PhyRegOprand r11 = new PhyRegOprand("r11");
    public static PhyRegOprand r12 = new PhyRegOprand("r12");
    public static PhyRegOprand r13 = new PhyRegOprand("r13");
    public static PhyRegOprand r14 = new PhyRegOprand("r14");
    public static PhyRegOprand r15 = new PhyRegOprand("r15");

    public static RegOprand getReg(String name, boolean isTemp, int depth) {
        if (depth != -1) name += "_" + Integer.toString(depth);
        if (virRegs.containsKey(name)) {
            return virRegs.get(name);
        } else {
            RegOprand newReg = new RegOprand(name, isTemp);
            virRegs.put(name, newReg);
            return newReg;
        }
    }

    public static void init() {
        virRegs = new HashMap<>();

        callerSave = new ArrayList<>();
        calleeSave = new ArrayList<>();
        args = new ArrayList<>();

        callerSave.add(rax);
        callerSave.add(rcx);
        callerSave.add(rdx);
        callerSave.add(rsi);
        callerSave.add(rdi);
        callerSave.add(r8);
        callerSave.add(r9);
        callerSave.add(r10);
        callerSave.add(r11);

        calleeSave.add(rbx);
        calleeSave.add(r12);
        calleeSave.add(r13);
        calleeSave.add(r14);
        calleeSave.add(r15);

        args.add(rdi);
        args.add(rsi);
        args.add(rdx);
        args.add(rcx);
        args.add(r8);
        args.add(r9);
    }
}
