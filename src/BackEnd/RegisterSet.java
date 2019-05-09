package BackEnd;

import OprandClass.PhyRegOprand;
import OprandClass.RegOprand;

import java.util.ArrayList;
import java.util.HashMap;

public class RegisterSet {
    public static HashMap<String, RegOprand> virRegs;

    public static ArrayList <PhyRegOprand> allRegs;
    public static ArrayList <PhyRegOprand> callerSave;
    public static ArrayList <PhyRegOprand> calleeSave;
    public static ArrayList <PhyRegOprand> args;

    public static PhyRegOprand rax;
    public static PhyRegOprand rcx;
    public static PhyRegOprand rdx;
    public static PhyRegOprand rbx;
    public static PhyRegOprand rsp;
    public static PhyRegOprand rbp;
    public static PhyRegOprand rsi;
    public static PhyRegOprand rdi;
    public static PhyRegOprand r8;
    public static PhyRegOprand r9;
    public static PhyRegOprand r10;
    public static PhyRegOprand r11;
    public static PhyRegOprand r12;
    public static PhyRegOprand r13;
    public static PhyRegOprand r14;
    public static PhyRegOprand r15;

    public static void init() {
        virRegs = new HashMap<>();

        allRegs = new ArrayList<>();
        callerSave = new ArrayList<>();
        calleeSave = new ArrayList<>();
        args = new ArrayList<>();

        String[] regNames = new String[] { "rax", "rcx", "rdx", "rbx", "rsp", "rbp", "rsi", "rdi", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"};
        Boolean[] isCaller = new Boolean[] { true, true,  true, false,  null,  null,  true,  true, true, true,  true,  true, false, false, false, false};

        for (int i = 0 ; i < 16 ; ++ i) {
            PhyRegOprand p = new PhyRegOprand(regNames[i]);
            allRegs.add(p);
            if (isCaller[i] != null) {
                if (isCaller[i]) {
                    callerSave.add(p);
                } else {
                    calleeSave.add(p);
                }
            }
        }

        rax = allRegs.get(0);
        rcx = allRegs.get(1);
        rdx = allRegs.get(2);
        rbx = allRegs.get(3);
        rsp = allRegs.get(4);
        rbp = allRegs.get(5);
        rsi = allRegs.get(6);
        rdi = allRegs.get(7);
        r8  = allRegs.get(8);
        r9  = allRegs.get(9);
        r10 = allRegs.get(10);
        r11 = allRegs.get(11);
        r12 = allRegs.get(12);
        r13 = allRegs.get(13);
        r14 = allRegs.get(14);
        r15 = allRegs.get(15);

        args.add(rdi);
        args.add(rsi);
        args.add(rdx);
        args.add(rcx);
        args.add(r8);
        args.add(r9);
    }

    public static RegOprand getReg(String name, boolean isTemp) {
        if (virRegs.containsKey(name)) {
            return virRegs.get(name);
        } else {
            RegOprand newReg = new RegOprand(name, isTemp);
            virRegs.put(name, newReg);
            return newReg;
        }
    }
}
