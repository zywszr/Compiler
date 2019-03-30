package TypeDefition;

public class ArrayTypeDef extends VarTypeDef {
    VarTypeDef type;
    FuncTypeDef getSize;

    public ArrayTypeDef(VarTypeDef typ) {
        type = typ;
    }

    public ArrayTypeDef(VarTypeDef typ, int dim) {
        VarTypeDef tmp = typ;
        for (int i = 1 ; i < dim ; ++ i) {
            tmp = new ArrayTypeDef(tmp);
        }
        type = tmp;
        getSize = new FuncTypeDef(new IntTypeDef());
    }

    public SingleTypeDef getSingleType() {
        if (type instanceof  SingleTypeDef) {
            return (SingleTypeDef)type;
        } else {
            return ((ArrayTypeDef)type).getSingleType();
        }
    }

    public VarTypeDef getInnerType() {
        return type;
    }

     @Override public ArrayTypeDef copy() {
        return new ArrayTypeDef(type);
     }

    @Override public boolean equals(TypeDef other) {
        if (other instanceof ArrayTypeDef) {
            return type.equals(((ArrayTypeDef) other).type);
        }
        return false;
    }
}
