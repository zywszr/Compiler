package TypeDefition;

public abstract class SpecialTypeDef extends SingleTypeDef {
    ClassTypeDef belong;

    public void setBelongClass(ClassTypeDef blg) {
        belong = blg;
    }

    public ClassTypeDef getBelongClass() {
        return belong;
    }

    @Override public abstract SpecialTypeDef copy();

    @Override public abstract boolean equals(TypeDef other);
}
