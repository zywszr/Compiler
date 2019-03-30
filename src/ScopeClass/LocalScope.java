package ScopeClass;

public class LocalScope<T> extends Scope<T> {
    public LocalScope(Scope<T> Parent, String Name) {
        super(Parent, Name);
    }
}
