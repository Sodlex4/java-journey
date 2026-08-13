# java-journey

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)

A hands-on Java Object-Oriented Programming journey — from OOP basics to modern Java features, one small runnable program at a time.

## Why

Each lesson is a self-contained, runnable program. Compile, run, read the code, then move on. Topics build on each other: object fundamentals → inheritance → composition → interfaces → the diamond problem → sealed classes → generics → dynamic dispatch → covariant returns. No frameworks, no boilerplate — just focused programs that demonstrate one concept each.

Each folder is a self-contained lesson with its own `main` method. Compile and run, read the code, then move on.

## Requirements

- **Java 21+** (the project targets Java 21)
- Maven (optional — only needed for the Eclipse/Maven project setup)
- Eclipse (optional — `.project`/`.classpath`/`.settings/` are included if you want to open it as a project)

## How to run

Compile the lesson file with `javac`, then run the class that contains `main` (note the main class is not always the file's first class):

```bash
javac 01-oop-basics/Book.java       && java -cp 01-oop-basics Book
javac 02-inheritance/inheritance.java && java -cp 02-inheritance Manager
javac 03-composition/Computer.java  && java -cp 03-composition Computer
javac 04-interfaces/Interfaces.java && java -cp 04-interfaces Interfaces
javac 05-diamond/DiamondProblem.java && java -cp 05-diamond DiamondProblem
javac 06-sealed/SealedClasses.java  && java -cp 06-sealed SealedClasses
javac 07-generics/Generics.java    && java -cp 07-generics Generics
javac 08-dispatch/Dispatch.java    && java -cp 08-dispatch Dispatch
javac 09-covariant/Covariant.java  && java -cp 09-covariant Covariant
```

Each folder compiles all of its classes at once; the entry class for each lesson is listed in the table below.

> Note: compiled `.class` files are ignored by git (`*.class` in `.gitignore`). They're regenerated automatically on every build — only the `.java` source is tracked.

## Lessons

### 01 — OOP Basics
`Book.java` — a `Book` class with a constructor, and overrides of `toString()`, `equals()`, and `hashCode()`. The main shows what happens when you compare two identical `Book` objects (hash codes and equality).

### 02 — Inheritance
`inheritance.java` — `Person` → `Employee` → `Manager` chain. Covers `extends`, the `super` keyword, method overriding (`speak()`, `toString()`), and how a subclass inherits and extends behavior.

### 03 — Composition
`Computer.java` — a `Computer` "has-a" `CPU`, `RAM`, and `HardDrive` (composition). Shows how objects are built from other objects instead of inheriting from them.

### 04 — Interfaces
`Interfaces.java` — abstract class `Teacher` plus `Coach` and `Researcher` interfaces. Shows `implements`, `extends` + `implements` together, abstract methods, and a class (`ScienceTeacher`) implementing multiple interfaces.

### 05 — Diamond Problem
`DiamondProblem.java` — two interfaces `A` and `B` both providing a `default hello()`. Class `C implements A, B` resolves the conflict with `A.super.hello()` — how Java solves the diamond problem.

### 06 — Sealed Classes
`SealedClasses.java` — `sealed` abstract class `Shape` permitting exactly `Circle`, `Square`, and `Triangle`, each `final`. Uses a `switch` pattern to describe any `Shape` — exhaustiveness guaranteed at compile time.

### 07 — Generics
`Generics.java` — `Box<T>` (generic class), `NumericBox<T extends Number>` (bounded type parameter), and a `Counter` helper with a generic method `<T> countOf`, plus wildcards (`List<? extends Number>`, `List<?>`). Shows type safety and how to read/write through wildcard bounds.

### 08 — Dynamic Dispatch
`Dispatch.java` — `A` → `B` → `C` chain where each class overrides `go()`. Shows that the *actual* runtime type decides which method runs, not the declared reference type. `bOnly()` (defined in `B`) is inherited by `C` but invisible through an `A` reference — compile-time visibility vs runtime dispatch.

### 09 — Covariant Return Types
`Covariant.java` — `Animal` and `Dog` where `Dog.baby()` overrides `Animal.baby()` but returns a `Dog` (a subtype) instead of `Animal`. No cast needed on the caller side. Also shows a covariant override of an interface method, and uses reflection to reveal the synthetic bridge method the compiler generates (`create()` returning `Animal`).

## Project layout

| Path | Topic | Entry class |
|------|-------|-------------|
| `01-oop-basics/` | Classes, constructors, `toString`/`equals`/`hashCode` | `Book` |
| `02-inheritance/` | Inheritance and overriding | `Manager` |
| `03-composition/` | Composition ("has-a") | `Computer` |
| `04-interfaces/` | Interfaces and abstract classes | `Interfaces` |
| `05-diamond/` | The diamond problem and default methods | `DiamondProblem` |
| `06-sealed/` | Sealed classes and pattern-matching switch | `SealedClasses` |
| `07-generics/` | Generic classes, bounded types, wildcards | `Generics` |
| `08-dispatch/` | Dynamic dispatch (runtime polymorphism) | `Dispatch` |
| `09-covariant/` | Covariant return types, bridge methods | `Covariant` |
| `pom.xml` | Maven project definition (Java 21) | — |
