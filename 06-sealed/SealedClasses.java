sealed abstract class Shape permits Circle, Square, Triangle {
    abstract double area();
}

final class Circle extends Shape {
    double radius;
    Circle(double radius) { this.radius = radius; }
    @Override
    public double area() { return Math.PI * radius * radius; }
}

final class Square extends Shape {
    double side;
    Square(double side) { this.side = side; }
    @Override
    public double area() { return side * side; }
}

final class Triangle extends Shape {
    double base, height;
    Triangle(double base, double height) { this.base = base; this.height = height; }
    @Override
    public double area() { return 0.5 * base * height; }
}

public class SealedClasses {
    public static void main(String[] args) {
        Shape s = new Circle(5);
        System.out.println(describe(s));   // → "Circle"

        Shape t = new Triangle(4, 3);
        System.out.println(describe(t));   // → "Triangle"
    }

    static String describe(Shape shape) {
        return switch (shape) {
            case Circle c -> "Circle, area=" + c.area();
            case Square q -> "Square, area=" + q.area();
            case Triangle t -> "Triangle, area=" + t.area();
        };
    }
}
