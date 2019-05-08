package OprandClass;

public class StrOprand extends ImmOprand {
    public StrOprand(Long _val) {
        super(_val);
    }
    @Override public void print() {
        System.out.print("S_" + Long.toString(val));
    }
}
