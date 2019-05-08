package ScopeClass;

import TypeDefition.*;
import javafx.util.Pair;
import java.util.*;

public class Scope<T> {
    public ArrayList< Scope<T> > childScopes;
    public Scope<T> parent;
    public SymbolTable<T> table;
    public ArrayList <String> varIdx;
    String name;

    public String getName() {
        return name;
    }

    public Scope(Scope<T> Parent, String Name) {
        childScopes = new ArrayList<>();
        parent = Parent;
        table = new SymbolTable<>();
        varIdx = new ArrayList<>();
        name = Name;
    }

    public void addChildScope(Scope<T> child) {
        childScopes.add(child);
    }

    public boolean addItem(String Name, T symbol) {
        return table.insert(Name, symbol);
    }

    public T findItem(String Name) {
        return table.findtarget(Name);
    }

    public static <K> ClassScope<K> newClassScope(Scope<K> Parent) {
        ClassScope<K> child = new ClassScope<>(Parent, Parent.name + '.' + Integer.toString(Parent.childScopes.size()));
        Parent.addChildScope(child);
        return child;
    }

    public static <K> LocalScope<K> newLocalScope(Scope<K> Parent) {
        LocalScope<K> child = new LocalScope<>(Parent, Parent.name + '.' + Integer.toString(Parent.childScopes.size()));
        Parent.addChildScope(child);
        return child;
    }

    public Pair<Scope<T>, T> matchVarName(String Name) {
        Scope<T> p = this;
        while (p != null) {
            T ret = p.table.findtarget(Name);
            if (ret != null) return new Pair<>(p, ret);
            p = p.parent;
        }
        return null;
    }

    boolean checkAddrType(T type) {
        return (type instanceof ArrayTypeDef) || (type instanceof SpecialTypeDef);
    }

    public void addVar(String varName, T type) {
        String Name = (checkAddrType(type) ? "A" : "V") + "_" + varName + "_" + name;
        // System.out.println(Name);
        varIdx.add(Name);
    }
}
