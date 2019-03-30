package ScopeClass;

import TypeDefition.ClassTypeDef;
import javafx.util.Pair;
import java.util.*;

public class Scope<T> {
    public ArrayList< Scope<T> > childScopes;
    public Scope<T> parent;
    public SymbolTable<T> table;
    String name;

    public String getName() {
        return name;
    }

    public Scope(Scope<T> Parent, String Name) {
        childScopes = new ArrayList<>();
        parent = Parent;
        table = new SymbolTable<>();
        name = Name;
    }

    public void addChildScope(Scope<T> child) {
        childScopes.add(child);
        return;
    }

    public boolean addItem(String Name, T symbol) {
        return table.insert(Name, symbol);
    }

    public T findItem(String Name) {
        return table.findtarget(Name);
    }


    public static <K> ClassScope<K> newClassScope(Scope<K> Parent) {
        ClassScope<K> child = new ClassScope<>(Parent, Parent.name + '.' + Integer.toString(Parent.childScopes.size()));
        Parent.childScopes.add(child);
        return child;
    }

    public static <K> LocalScope<K> newLocalScope(Scope<K> Parent) {
        LocalScope<K> child = new LocalScope<>(Parent, Parent.name + '.' + Integer.toString(Parent.childScopes.size()));
        Parent.childScopes.add(child);
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
}
