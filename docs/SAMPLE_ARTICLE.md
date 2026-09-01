# Sample article content

Paste the block below into the **Content (Markdown)** field of the admin article form to
check that headings, images, tables, lists, blockquotes and Java code all render.

Suggested values for the rest of the form:

- Title: `Inheritance in Java`
- Category: `Object Oriented Programming`
- Description: `Understanding inheritance in Java with real world examples.`
- Tags: `java, oop, inheritance`
- GitHub URL: `https://github.com/yourusername/java-examples`
- Published: ticked

---

## What is Inheritance?

Inheritance lets one class acquire the properties and behaviour of another class.
It is the mechanism behind code reuse and hierarchical relationships in Java.

## Real World Example

A Car **is a** Vehicle. Everything a Vehicle can do, a Car can do as well, and a Car adds
behaviour of its own.

> Use inheritance for an is-a relationship. Use composition for a has-a relationship.

## Java Example

```java
class Vehicle {
    void start() {
        System.out.println("Vehicle started");
    }
}

class Car extends Vehicle {
    void drive() {
        System.out.println("Car is driving");
    }
}
```

## Types of Inheritance

| Type         | Supported by classes | Supported by interfaces |
|--------------|----------------------|-------------------------|
| Single       | Yes                  | Yes                     |
| Multilevel   | Yes                  | Yes                     |
| Hierarchical | Yes                  | Yes                     |
| Multiple     | No                   | Yes                     |

## Key Points

- Promotes code reuse
- Supports hierarchical relationships
- Uses `extends` for classes and `implements` for interfaces
- Java has no multiple inheritance of state, only of type

## Interview Questions

1. Why does Java not support multiple inheritance of classes?
2. What is the difference between inheritance and composition?
3. What happens when a subclass overrides a private method?

## Images

An uploaded image is referenced by the path returned from the upload endpoint:

```markdown
![Inheritance diagram](/uploads/articles/your-uploaded-file.png)
```

The article page rewrites that path to the API host before displaying it.
