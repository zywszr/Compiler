package IRClass;

import FrontEnd.Node.*;
import OprandClass.Oprand;
import OprandClass.StackSlot;
import TypeDefition.ArrayTypeDef;
import org.antlr.v4.misc.Utils;

import java.util.*;

public class FuncFrame {
    ArrayList <CFGNode> nodes;
    ArrayList <CFGNode> ends;
    ArrayList <CFGNode> cfgList;
    CFGNode start;
    CFGNode end;
    String name;
    HashMap <String, Long> varIdx;
    Long varSize;

    public LinkedList <StackSlot> params = new LinkedList<>();
    public LinkedList <StackSlot> temps = new LinkedList<>();

    public LinkedList <Oprand> parameters;
    public HashSet <Oprand> phyRegs;

    public int getFuncSize() {
        int siz = 8 * (params.size() + temps.size());
        siz = (siz + 16 - 1) / 16 * 16;
        return siz;
    }


    public FuncFrame(String _name) {
        nodes = new ArrayList<>();
        ends = new ArrayList<>();
        cfgList = new ArrayList<>();
        start = null;
        end = null;
        name = _name;
        varIdx = new HashMap<>();
        varSize = 0L;
        parameters = new LinkedList<>();
        phyRegs = new HashSet<>();
    }

    public void pushVar(String _name) {
        varIdx.put(_name, varSize);
        varSize += 1;
    }

    public void pushLabel(CFGNode _label) {
        nodes.add(_label);
    }

    public CFGNode getStart() {
        return start;
    }

    public CFGNode getEnd() {
        return end;
    }

    public void setEnd(CFGNode _end) {
        end = _end;
    }

    public void createCFG() {
        for (CFGNode label : nodes) {
            label.froms.clear();
        }
        start = nodes.get(0);
        Queue <CFGNode> q = new LinkedList<>();
        HashMap <String, Long> tmp = new HashMap<>();
        q.offer(start);
        tmp.put(start.getName(), 1L);
        while (!q.isEmpty()) {
            CFGNode top = q.poll();
            top.setIdx(cfgList.size());
            cfgList.add(top);
            if (top.tos.isEmpty()) {
                ends.add(top);
            }
            for (CFGNode to : top.tos) {
                to.addFrom(top);
                if (tmp.get(to.getName()) == null) {
                    q.offer(to);
                    tmp.put(to.getName(), 1L);
                }
            }
        }
    }

    public void print() {
        System.out.println("FuncDef " + name + ":");
        for (int i = 0 ; i < cfgList.size() ; ++ i) {
            cfgList.get(i).print();
        }
        System.out.println("FuncDef Done");
        System.out.println();
    }

    public void printCode() {
        System.out.println(name + ":");
        for (int i = 0 ; i < cfgList.size() ; ++ i) {
            cfgList.get(i).printCode();
        }
        System.out.println();
    }

    public ArrayList <CFGNode> getCfgList() {
        return cfgList;
    }

    public String getName() {
        return name;
    }
}
