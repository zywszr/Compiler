package OprandClass;

public class ArrayAddrOprand extends AddrOprand {
    public ArrayAddrOprand(Oprand _base, Oprand _offset, Oprand _scale) {
        super(_base, _offset, _scale);
        disp = new ImmOprand(8L);
    }

    @Override public void print() {
        System.out.print("[");
        base.print();
        if (offSet != null) {
            System.out.print("+");
            offSet.print();
            System.out.print("*");
            scale.print();
        }
        System.out.print("+8]");
    }
}
