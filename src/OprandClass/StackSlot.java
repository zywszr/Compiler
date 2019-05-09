package OprandClass;

public class StackSlot extends MemOprand {
    public StackSlot() {
        super(null, null, null);
    }

    @Override public void print() {
        if (base == null) {
            System.out.print("stack[]");
        } else {
            // ((MemOprand) this).print();
            System.out.print("qword ");
            System.out.print("[");
            base.print();
            if (offSet != null) {
                System.out.println("+");
                offSet.print();
                System.out.println("*");
                scale.print();
            }
            if (disp != null) {
                System.out.print("+");
                disp.print();
            }
            System.out.print("]");
        }
    }
}
