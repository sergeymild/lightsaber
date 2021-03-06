[![Build Status](https://travis-ci.org/MichaelRocks/lightsaber.svg?branch=develop)](https://travis-ci.org/MichaelRocks/lightsaber)

Lightsaber
==========

Compile time dependency injection framework for JVM languages. Especially for Kotlin.

Why?
----

This framework is inspired by two projects: Guice and Dagger. While Guice is a quite small but a very powerful library
it's not efficient enough on Android as it relies on reflection at runtime. On the other hand Dagger makes all
its magic at compile time and thus is very efficient. However, Dagger uses APT under the hood, what may become a problem
when used not from Java.

The goal of Lightsaber is to provide lightning-fast compile time dependency injection and not to rely on APT at the same
time so that the library can be used with almost any JVM language and on Android.

Usage
-----

### Configuration

```groovy
buildscript {
  repositories {
    jcenter()
  }

  dependencies {
    classpath 'io.michaelrocks:lightsaber-gradle-plugin:0.9.0-beta'
  }
}

// For Android projects.
apply plugin: 'com.android.application'
apply plugin: 'io.michaelrocks.lightsaber.android'

// For other projects.
apply plugin: 'java'
apply plugin: 'io.michaelrocks.lightsaber'

// Optional, just if you need Kotlin extension functions.
dependencies {
  compile 'io.michaelrocks:lightsaber-core-kotlin:0.9.0-beta'
}
```

### Declaring dependencies

The primary goal of a DI framework is to inject dependencies into your code. Lightsaber can do that with constructor,
field, and method injection. In order to make injection work you have to annotate a method or a field with the `@Inject`
annotation and [provide dependencies](#providing-dependencies) in other parts of the project.

#### Constructor injection

Constructor injection is the most proper way of performing injection. All you have to do is to annotate a constructor of
a class with `@Inject`. Lightsaber will be able to provide values for the arguments of the constructor and to create
an instance of the class using this constructor. Moreover, when using constructor injection the class becomes eligible
for provision, that is this class itself can be used as a dependency. Lightsaber requires neither the class, nor the
injectable constructor to be `public`.

```java
public class Droid {
  @Inject
  public Droid(Battery battery) {
  }
}
```

#### Field injection

Sometimes you don't manage instantiation of a class. In this case you cannot use constructor injection. But you can
still use dependency injection for such classes. The easiest way to do that is to inject dependencies right into fields
of your class. To inform Lightsaber which fields it needs to inject you have to annotate them with `@Inject`.
Again, Lightsaber doesn't require the injectable field to be `public` or `final`.

```java
public class Droid {
  @Inject
  private Battery battery;
}
```

#### Method injection

In some cases you may want Lightsaber to call a method of a class after all fields of the class have been injected.
Just annotate the method with `@Inject` and Lightsaber will provide values for the arguments of the method and invoke
it. And as always, Lightsaber doesn't need the method to be `public`.

```java
public class Droid {
  private Battery battery;

  @Inject
  public void setBattery(Battery battery) {
    this.battery = battery;
  }
}
```

#### Injection order

Let's assume there's a class with constructor, fields, and methods marked as injectable . This class may have ancestor
classes with injectable fields and methods. When instantiating this class Lightsaber will perform injection in the
following order.

1. Instantiate the class via its injectable constructor.
2. Inject fields starting from ancestor classes.
3. Invoke injectable methods starting from ancestor classes. The order of injectable method invocations is undefined.

### Providing dependencies

In order to be able to inject a dependency you have to provide this dependency first. In other words you have to tell
Lightsaber what it have to return when requested a dependency of some type. This can be done in two ways: using modules
and their provider methods and via injectable constructors mentioned earlier.

#### Provider methods

Lightsaber requires provider methods to be defined in modules that need to be combined into components.

##### Modules

A module is a logical unit responsible for providing dependencies belonging to the module. Module classes must be
annotated with the `@Module` annotation. A module can contain a number of provider methods. Lightsaber treats a method
as a provider method if it's annotated with the `@Provides` annotation. When a type is provided by a provider method
it can be injected into a class in other parts of the project. Neither the module nor its provider methods are required
to be `public`.

```java
@Module
public class DroidModule {
  @Provides
  public Droid provideDroid() {
    return new Droid();
  }
}
```

Note that when manually creating a dependency Lightsaber doesn't perform field and method injection into the returned
instance. But you can do that via [manual injection](#manual-injection) or by creating a dependency with an
[injectable constructor](#injectable-constructors).

##### Components

To make Lightsaber aware of modules and their provided dependencies the modules have to be organized into a component.
A component is just a class annotated with the `@Component` annotation. The goal of this class is to provide modules
to Lightsaber. Every method that provides a module must be annotated with `@Provides`. Neither the component class
itself not its provider methods have to be `public`.

```java
@Component
public class DroidComponent {
  @Provides
  public DroidModule provideDroidModule() {
    return new DroidModule();
  }
}
```

One of the reasons why you need a component is that its instance should be passed as an arguments to a method that
creates an `Injector`. Finally, when a component is defined you can create an injector with this component.

```
Injector injector = Lightsaber.get().createInjector(DroidComponent());
```

The `createInjector()` method accepts a single component and returns an injector that can provide any dependency from
any module of the component and from any class with an [injectable constructor](#injectable-constructors).

#### Injectable constructors

A class may have one and only one injectable constructor. This constructor must be annotated with `@Inject` and can
have any number of arguments. When instantiating a class with an injectable constructor via an injector the injector
must be able to provide instances for every argument of the constructor.

```java
public class Droid {
  @Inject
  public Droid(Battery battery) {
  }
}
```

When providing a dependency using an injectable constructor Lightsaber will perform field and method injection into
the provided instance.

### Manual injection

Manual injection is a way to create an instance of a provided type or to perform field and method injection into an
existing object. An instance can be obtained by calling the `getInstance()` method of the `Injector`:

```
Droid droid = injector.getInstance(Droid.class);
```

If you need a factory that provides instances of a given type you can get a `Provider` object from the `Injector`.
Then you'll be able to get an instance from the `Provider` by calling its `get()` method: 

```
Provider<Droid> droidProvider = injector.getProvider(Droid.class);
Droid droid = droidProvider.get();
```

When creating an instance of a dependency manually Lightsaber performs field and method injection for this instance.
But sometimes you already have an instance and want to inject dependencies into it. You can do that by calling the
`injectMember()` method of the `Injector` passing the instance to it.

```java
public class DroidController {
  @Inject
  private Droid droid;

  public void initialize(Injector injector) {
    injector.injectMembers(this);
  }
}
```

Consider the following example. We have a `Droid` interface and its implementation and we want to provide `Droid` as a
dependency.

```java
public interface Droid {
  /* ... */
}

public class ElectricalDroid implements Droid {
  @Inject
  private Battery battery;

  /* ... */
}
```

If we just create an `ElectricalDroid` instance and return it from a provider method the `battery` field will not be
initialized because Lightsaber doesn't perform injection into instances it doesn't manage. But we can fix that by
manually injecting dependencies into the instance using the `injectMembers()` method.

```java
@Module
public class DroidModule {
  @Provides
  public Droid provideDroid(Injector injector) {
    Droid droid = new ElectircalDroid();
    injector.injectMemebers(droid);
    return injector;
  }
}
```

While this is a working example it can be refactored to using constructor injection. In this case manual injection
becomes unnecessary.

```java
class ElectricalDroid implements Droid {
  private Battery battery;

  @Inject
  public ElectricalDroid(Battery battery) {
    this.battery = battery;
  }

  /* ... */
}
```

```java
@Module
public class DroidModule {
  @Provides
  public Droid provideDroid(ElectricalDroid droid) {
    return droid;
  }
}
```

### Singleton injection

By default Lightsaber creates a new instance every time a dependency is requested. This behavior can be changed so that
Lightsaber will return a single instance of the dependency for a given injector. All you need to do is to apply the
`@Singleton` annotation to a class with an injectable constructor or to a provider method.

```java
@Singleton
class ElectricalDroid implements Droid {
  /* ... */
}
```

```java
@Module
public class DroidModule {
  @Provides
  @Singleton
  public Droid provideDroid(ElectricalDroid droid) {
    return droid;
  }
}
```

In the example above you can annotate just a class or just a provider method or both the class and the provider method
with the `@Singleton` annotation and behavior will be very similar but not exactly the same.

If the `ElectricalDroid` is a singleton then one and only one instance of this class will be created per an injector
instance. And even if the `provideDroid()` method is not annotated with `@Singleton` it will return the same instance
every time it's called because it returns a singleton instance of `ElectricalDroid`.

On the other hand, if the `ElectricalDroid` class isn't a singleton the `provideDroid()` method annotated with
`@Singleton` will return a cached instance of `ElectricalDroid` so the instance will always be the same. But if
`ElectricalDroid` is injected somewhere else a new instance of this class will be created.

### Lazy injection

Instead of creating a dependency instance at injection time its instantiation can be deferred until the object is really
needed. For this purpose Lightsaber has a generic `Lazy` interface that can be injected instead of the dependency.

```java
public class Droid {
  @Inject
  private Lazy<Battery> battery;

  public void charge() {
    battery.get().charge();
  }
}
```

In this example a `Battery` instance will be created only when `battery.get()` is called.

### Provider injection

Provider injection is somewhat similar to lazy injection with one major difference: when `Provider.get()` is called
multiple times you can receive either the same instance of a dependency or a different instance on each invocation of
the `get()` method. Provider injection is useful when you need to pass some arguments to a constructor of an object
while other arguments should be provider by an injector.

```java
public class Droid {
  public Droid(Battery battery, Adapter adapter) {
    /* ... */
  }
}
```

```java
public class DroidFactory {
  @Inject
  private Provider<Battery> batteryProvider;

  public Droid createDroidWithAdapter(Adapter adapter) {
    return new Droid(batteryProvider.get(), adapter);
  }
}
```

### Qualified injection

Sometimes you may want to provide different implementations of a single dependency type. You can do that by applying a
qualifier annotation to a class with an injectable constructor or to a provider method. Then you need to apply the same
qualifier annotation to the provided dependency at the injection point. A dependency may have either no qualifiers or a
single one.

In the next example we will create a module that provides two different instances of the `Droid` class. To make
Lightsaber distinguish between these dependencies we will annotate them with the built-in `@Named` qualifier.

```java
@Module
public class DroidModule {
  @Provides
  @Singleton
  @Named("R2-D2")
  public Droid provideR2D2() {
    return new Droid("R2-D2");
  }

  @Provides
  @Singleton
  @Named("C-3PO")
  public Droid provideC3PO() {
    return new Droid("C-3PO");
  }

  @Provides
  @Singleton
  public Droid provideUnknownDroid() {
    return new Droid("Unknown");
  }
}
```

```java
public class DroidParty {
  @Inject
  @Named("R2-D2")
  private Droid r2d2;

  @Inject
  @Named("C-3PO")
  private Droid c3po;

  @Inject
  private Droid unknownDroid;
}
```

#### Custom qualifiers

Besides using the `@Named` qualifier you can create you own one. To do that you need to create an annotation and
annotate it with the `@Qualifier` annotation.

```java
public enum DroidType { R2D2, C3PO }

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.TYPE,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER
})
public @interface Model {
  DroidType value();
}
```

```java
@Module
public class DroidModule {
  @Provides
  @Singleton
  @Model(DroidType.R2D2)
  public Droid provideR2D2() {
    return new Droid("R2-D2");
  }

  @Provides
  @Singleton
  @Model(DroidType.C3PO)
  public Droid provideC3PO() {
    return new Droid("C-3PO");
  }
}
```

```java
public class DroidParty {
  @Inject
  @Model(DroidType.R2D2)
  private Droid r2d2;

  @Inject
  @Model(DroidType.C3PO)
  private Droid c3po;
}
```

Custom qualifiers are allowed to have any number of properties of any type. When resolving dependencies Lightsaber
compares qualifiers by their types and equality of all their properties.

### Generic injection

With Lightsaber you can inject dependencies of generic types. The generic dependency has to be a parameterized type
and its type parameters cannot contain wildcards and type variables.

For example, these types you can use for injection:

- `List<String>`
- `Map<String, Collection<String>>`
- `Collection<int[]>`

And these types you cannot use:

- `List<? extends CharSequence>`
- `Map<String, T>`

### Child injection

When defining a component you can specify any number of parent components of the component. Given an injector created
with one of the parent components you can create a child injector by passing an instance of the child component to
the `createChildInjector()` method of the `Lightsaber` class.

The child injector inherits all the dependencies of its ancestor components, overrides the `Injector` dependency with
itself, and adds dependencies defined in its component. At the moment Lightsaber doesn't support dependency overriding
so all the components in a component chain must have distinct dependencies provided.

Consider the following case. In different parts of an application we need to construct droids. But depending on
a construction point we need to inject a battery of the corresponding type into a droid.

The following classes define a component that provides droids. Each droid accepts a `Battery` as a dependency.

```java
class ElectricalDroid implements Droid {
  private Battery battery;

  @Inject
  public ElectricalDroid(Battery battery) {
    this.battery = battery;
  }

  /* ... */
}
```

```java
@Module
public class DroidModule {
  @Provides
  @Singleton
  public Droid provideDroid(ElectricalDroid droid) {
    return droid;
  }
}
```

```java
@Component
public class DroidComponent {
  @Provides
  public DroidModule provideDroidModule() {
    return new DroidModule();
  }
}
```

As you can see the `Battery` is not provided anywhere. Here's our trivial `Battery` class.

```java
public class Battery {
  private String name;

  public Battery(String name) {
    this.name = name;
  }

  /* ... */
}
```

Let's define a component that provides a `Battery` with a given name.

```java
@Module
public class BatteryModule {
  private String name;

  public BatteryModule(String name) {
    this.name = name;
  }

  @Provides
  public Droid provideBattery() {
    return new Battery(name);
  }
}
```

```java
@Component(parents = { DroidComponent.class })
public class BatteryComponent {
  private String name;

  public BatteryComponent(String name) {
    this.name = name;
  }

  @Provides
  public BatteryModule provideBatteryModule() {
    return new BatteryModule(name);
  }
}
```

Now we can create child injectors passing different instances of the `BatteryComponent` class to the
`createChildInjector()` method.

```
Injector droidInjector = Lightsaber.get().createInjector(new DroidComponent());
Injector nuclearBatteryInjector =
    Lightsaber.get().createChildInjector(droidInjector, new BatteryComponent("Nuclear"));
Injector plasmBatteryInjector =
    Lightsaber.get().createChildInjector(droidInjector, new BatteryComponent("Plasm"));

Droid nuclearBatteryDroid = nuclearBatteryInjector.getInstance(Droid.class);
Droid plasmBatteryDroid = plasmBatteryInjector.getInstance(Droid.class);
```

In the example above we created two singleton instances of the `ElectricalDroid` class passing different instances of
the `Battery` class to them. Please, note that if for some reason a singleton dependency was instantiated via a parent
injector and then child injectors were created the child injectors would return the same singleton instance created by
the parent injector.

License
-------

    Copyright 2016 Michael Rozumyanskiy

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
