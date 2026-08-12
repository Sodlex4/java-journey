class A {
    void go() { System.out.println("A.go"); }
}

class B extends A {
    @Override
    void go() { System.out.println("B.go"); }
    void bOnly() { System.out.println("B's own method"); }
}

class C extends B {
    @Override
    void go() { System.out.println("C.go"); }
}

public class Dispatch {
    public static void main(String[] args) {
        A a = new A();
        a.go();                  // → A.go

        A refB = new B();
        refB.go();               // → B.go  (actual type wins, not declared type)

        A refC = new C();
        refC.go();               // → C.go  (deepest override wins)

        B b = new C();
        b.go();                  // → C.go
        b.bOnly();               // → B's own method  (inherited by C)

        // refC.bOnly();         // compile error: bOnly() not visible through A
    }
}
