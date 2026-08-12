import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Box<T> {
    private T value;

    Box(T value) { this.value = value; }

    T get() { return value; }
    void set(T value) { this.value = value; }

    @Override
    public String toString() { return "Box[" + value + "]"; }
}

class NumericBox<T extends Number> {
    private final T value;

    NumericBox(T value) { this.value = value; }

    double doubleValue() { return value.doubleValue(); }
}

class Counter {
    static <T> int countOf(List<T> list, T target) {
        int count = 0;
        for (T item : list) {
            if (item.equals(target)) count++;
        }
        return count;
    }

    static double sumAll(List<? extends Number> numbers) {
        double total = 0;
        for (Number n : numbers) total += n.doubleValue();
        return total;
    }

    static void printAll(List<?> items) {
        System.out.println(items);
    }
}

public class Generics {
    public static void main(String[] args) {
        Box<String> name = new Box<>("Stephen");
        name.set("Odonde");
        System.out.println(name);               // → Box[Odonde]

        Box<Integer> count = new Box<>(42);
        System.out.println(count.get() + 1);    // → 43

        NumericBox<Double> weight = new NumericBox<>(72.5);
        System.out.println(weight.doubleValue());   // → 72.5

        List<String> names = new ArrayList<>(Arrays.asList("a", "b", "a"));
        System.out.println(Counter.countOf(names, "a"));    // → 2

        List<Integer> ints = List.of(1, 2, 3);
        System.out.println(Counter.sumAll(ints));           // → 6.0

        Counter.printAll(ints);                             // → [1, 2, 3]
    }
}
