abstract class Teacher {
  String name;

  void introduce() {
    System.out.println("I'm " + name);
  }

  abstract void teach();
}

interface Coach {
  void train();
}

interface Researcher {
  void publish();
}

class MathTeacher extends Teacher implements Coach {
  MathTeacher(String name) {
    this.name = name;
  }

  void teach() {
    System.out.println(name + " teaches Math: 2 + 2 = 4");

  }

  public void train() {
    System.out.println(name + " coaches Math Olympiad");
  }
}

class EnglishTeacher extends Teacher implements Researcher {
  EnglishTeacher(String name) {
    this.name = name;
  }

  void teach() {
    System.out.println(name + " teaches English: Grammar");
  }

  public void publish() {
    System.out.println(name + " publishes English papers");
  }
}

class ScienceTeacher extends Teacher implements Coach, Researcher {
  ScienceTeacher(String name) {
    this.name = name;
  }

  void teach() {
    System.out.println(name + " teaches Science: H2O is water");
  }

  public void train() {
    System.out.println(name + " coaches Science fair");
  }

  public void publish() {
    System.out.println(name + " publishes Science research");
  }
}

public class Interfaces {
  public static void main(String[] args) {
    MathTeacher math = new MathTeacher("Alice");
    EnglishTeacher eng = new EnglishTeacher("Bob");
    ScienceTeacher sci = new ScienceTeacher("Charlie");

    System.out.println("--- Math Teacher ---");
    math.introduce();
    math.teach();
    math.train();

    System.out.println();

    System.out.println("--- English Teacher ---");
    eng.introduce();
    eng.teach();
    eng.publish();

    System.out.println();

    System.out.println("--- Science Teacher ---");
    sci.introduce();
    sci.teach();
    sci.train();
    sci.publish();
  }
}
