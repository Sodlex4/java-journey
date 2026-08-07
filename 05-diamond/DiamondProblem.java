interface A {
    default void hello() { System.out.println("Hello from A"); }
}

interface B {
    default void hello() { System.out.println("Hello from B"); }
}

class C implements A, B {
    @Override
    public void hello() {
        A.super.hello();
    }
}

public class DiamondProblem {
    public static void main(String[] args) {
        C c = new C();
        c.hello();
    }
}
