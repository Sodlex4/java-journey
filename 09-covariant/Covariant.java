import java.lang.reflect.Method;

class Animal {
    void speak() { System.out.println("animal sound"); }

    Animal baby() { return new Animal(); }
}

class Dog extends Animal {
    @Override
    void speak() { System.out.println("woof"); }

    @Override
    Dog baby() { return new Dog(); }      // covariant return: Dog is a subtype of Animal

    void fetch() { System.out.println("fetch!"); }
}

interface AnimalFactory {
    Animal create();
}

class DogFactory implements AnimalFactory {
    @Override
    public Dog create() { return new Dog(); }   // covariant override of an interface method
}

public class Covariant {
    public static void main(String[] args) {
        Dog rex = new Dog();
        Dog puppy = rex.baby();          // no cast needed — the compile-time type is Dog
        puppy.fetch();                   // → fetch!

        // before Java 5 you'd have to cast:
        // Dog pup = (Dog) rex.baby();

        DogFactory factory = new DogFactory();
        Dog made = factory.create();     // covariant override lets this compile
        made.speak();                    // → woof

        System.out.println("--- bridge methods ---");
        for (Method m : DogFactory.class.getDeclaredMethods()) {
            System.out.println("create() returns " + m.getReturnType().getSimpleName()
                    + " | bridge=" + m.isBridge());
        }
        // the JVM needs create() returning Animal (the interface signature);
        // javac generates a synthetic "bridge" method that casts and calls the Dog version.
    }
}
