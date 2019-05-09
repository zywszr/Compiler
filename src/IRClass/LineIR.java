package IRClass;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class LineIR {
    ArrayList <FuncFrame> funcs;

    HashMap <String, Long> classSizeList;
    HashMap <String, Long> strLiters;
    ArrayList <String> globalVar;
    Long strLiterSize;
    ArrayList <Pair<String, String>> roData;

    Long globalVarSize;

    ArrayList <String> global;

    public LineIR() {
        funcs = new ArrayList<>();
        classSizeList = new HashMap<>();
        strLiters = new HashMap<>();
        globalVar = new ArrayList<>();
        strLiterSize = 0L;
        globalVarSize = 0L;
        global = new ArrayList<>();
        roData = new ArrayList<>();
    }

    public void pushClassSize(String className, Long Size) {
        classSizeList.put(className, Size);
    }

    public void pushGlobalVar(String varName) {
        globalVar.add(varName);
        globalVarSize += 1;
        global.add(varName);
    }

    public void pushFunc(FuncFrame func) {
        funcs.add(func);
        global.add(func.name);
    }

    public void addRoData(String name, byte[] list) {
        int len = list.length;
        String str = "";
        for (int i = 0 ; i < len ; ++ i) {
            String tmp = Integer.toHexString(list[i]).toUpperCase();
            if (tmp.length() == 1) tmp = '0' + tmp;
            str += tmp + "H, ";
        }
        str += "00H";
        roData.add(new Pair<>(name, str));
    }

    public ArrayList <Pair <String, String> > getRoData() {
        return roData;
    }

    public Long addStrLiter(String str) {
        /* Long ret = strLiters.get(str);
        if (ret != null) return ret;
        else */
        Long ret = strLiterSize;
        addRoData("S_" + Long.toString(ret), str.getBytes());
        strLiters.put(str, strLiterSize);
        strLiterSize += 1;
        return ret;
    }

    public void print() {
        for (int i = 0 ; i < funcs.size() ; ++ i) {
            funcs.get(i).print();
        }
    }

    public void printCode() {
        for (FuncFrame func : funcs) {
            func.printCode();
        }
    }

    public ArrayList <FuncFrame> getFuncs() {
        return funcs;
    }

    public ArrayList <String> getGlobal() {
        return global;
    }

    public ArrayList <String> getGlobalVar() {
        return globalVar;
    }
}
