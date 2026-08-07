class Person {
String name;
int age;

Person(String name, int age){
this.name = name;
this.age = age;
System.out.println("founder: "+  name);

}

void speak() {
System.out.println("Hello, I'm " + name);
}


void eat(){
System.out.println(name + " is eating");
}

@Override
public String toString(){
return "Person{name='" + name + "', age= " + age + "}";
}
}

class Employee extends Person {
double salary;

Employee(String name, int age, double salary){
super(name, age);
this.salary = salary;
}

void work(){
System.out.println(name + " is coding " );
}

@Override 
public String toString(){
return super.toString() + ", salary=" + salary;
}
}


class Manager extends Employee{
String department;

Manager(String name, int age, double salary, String department){
super(name, age, salary);
this.department = department;
}

void lead(){
System.out.println(name + " is leading the project ");
}

@Override
public String toString(){
return super.toString() + ", department=" + department;
}
 
@Override
void speak(){
super.speak();
System.out.println("I lead this," + department); 
}
public static void main(String[] args){
Manager b = new Manager("odonde", 50, 70000, "Engineering");

    b.speak();
    b.work();    
    b.lead();

    System.out.println(b);
}
}


 










