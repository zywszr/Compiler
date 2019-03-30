package TypeDefition;

public class StringTypeDef extends SpecialTypeDef {
    public StringTypeDef() {
        typeId = "string";
    }

    @Override public StringTypeDef copy() {
        return new StringTypeDef();
    }

    @Override public boolean equals(TypeDef other) {
        return (other instanceof StringTypeDef);
    }
}
