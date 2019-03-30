package TypeDefition;

public class VoidTypeDef extends SimpleTypeDef {
    public VoidTypeDef() {
        typeId = "void";
    }

    @Override public VoidTypeDef copy() {
        return new VoidTypeDef();
    }

    @Override public boolean equals(TypeDef other) {
        return (other instanceof VoidTypeDef);
    }
}
