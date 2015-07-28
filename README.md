# Agera

Agera is a set of classes and interfaces to help write functional, asynchronous, suspendable and 
reactive applications for Android.


## Functional Behaviors

Agera contains a few basic interfaces to help writing slightly more functional code than in 
traditional Java, and even more so than in idiomatic Android Java. These basic generically typed 
interfaces are:

* `Function` - Returns an object based a received value with `Function.apply(Object)`
* `Supplier` - Returns an object with `Supplier.get()`
* `Receiver` - Accepts and object with `Receiver.accept(Object)`
* `Merger` - Accepts two objects and returns a third with `Merger.merge(Object, Object)`
* `Binder` - Accepts two objects with `Binder.bind(Object, Object)`
* `Predicate` - Returns a boolean based on a received value with `Predicate.apply(Object)`
* `Condition` - Returns a boolean value with `Condition.applies()`

Most of these interfaces have a few helpful implementations in Agera, each created with the static 
factory methods available in `Functions`, `Suppliers`, `Receivers`, `Predicates` and `Conditions`.

The Agera functional interfaces assumes that `null` is never returned or received. They also assume 
that no exceptions are thrown. In order to handle these cases Agera provides the class `Result`.

Although these interfaces might help to create structure, they have very little value on their own, 
but are made available to interface with other parts of Agera.

### Compiling Functions

`Function`s are used excessively in Agera. To make it easier to write complex functions from 
smaller reusable functions Agera provides a function compiler.

The following contrived example shows how the function compiler can be used to create a complex 
`Function` using simpler functions:

    Function<String, String> function = Functions.functionFrom(String.class)
            .apply(new DoubleString()) // Doubles the string
            .unpack(new StringToListChar()) //Unpacks the string into a list of chars 
            .morph(new SortList<Character>()) // Takes the full list of chars and sorts them
            .limit(5) // Picks the first five items in the list
            .filter(new CharacterFilter('n')) // Picks all items matching the filter ('n' chars)
            .map(new NextChar()) // Applies NextChar to each character, getting the next ('o')
            .thenApply(new CharacterListToString()); // Converters the list back into a string

In the example a `Function` from `String` to `String` is created. The example, although itself not 
very useful, still demonstrates the capabilities of the function compiler. The Agera function 
compiler can both operate on single items and, when unpacked (or if the compiler is started from 
`functionFromListOf`), lists.

## Event Notifications

Agera provides a simple event notification system that implements the publisher-subscriber pattern, 
with the `Observable` interface being the publisher and the `Updatable` interface the subscriber. 
An `Observable` will notify any `Updatable` registered with `Observable.addUpdatable(Updatable)` 
of changes by calling `Updatable.update()`.

`Updatable`s has to be added and removed in balance. It is not allowed to add an `Updatable` more 
than once, and it is not allowed to remove one that was never added.

The `UpdateDispatcher` allows us to send notifications from `Observable` services to clients. 
However, the notifications themselves contains no data. The client only know that something 
happened, and not necessarily why it was notified. Agera mandates the following:

1. A client gets notified from services that something happened
2. It asks its services about their state
3. It then re-evaluates its own state based on the states supplied by the services
4. Optionally it notifies its own clients of changes in its own state

This has a few implications:

* Supplying values to clients needs to be synchronous and fast
* If no value is available, a _null object_ needs to be provided (the Agera classes `Maybe` or 
  `Try` could be used)

Consider the following Android `Activity`:

    public final class MyActivity extends Activity implements Updatable {
      private Observable observable;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        observable = MyObservableService.getInstance();
      }

      @Override
      protected void onResume() {
        super.onResume();
        observable.addUpdatable(this);
      }

      @Override
      protected void onPause() {
        super.onPause();
        observable.removeUpdatable(this);
      }

      @Override
      public void update() {
        //Service updated
      }
    }

The observable is obtained in `onCreate`, and the `Activity` is added as a client to the
`Observable` in `onResume`. As the `Observable` is updated, `Updatable.update()` in the `Activity` 
is called.

Note that the `Activity` in the example removes itself from the `Observable` using 
`Observable.removeUpdatable(Updatable)` in `onPause`. If this is not done, one of two things might 
happen:

1. The `Observable` and `Activity` will keep references to each other forever, and the application 
   will leak memory
2. The `Activity` is added to the same `Observable` again, causing the `Observable` to throw an 
   exception

## Repositories

Implementing an `Observable` service that supplies a value is so common it has its own interface, 
`Repository`. `Repository` is an `Observable` with a value that updates when it changes, and the 
value can be fetched using `Supplier.get()`.

As with the other Agera interfaces, static factory methods providing useful implementations of 
`Repository` are available in `Repositories`. The most simple one is _static_, supplying values 
but never sending notifications (since the values never changes).

`MutableRepository` is a `Repository` that also implements the `Receiver` interface, which allows 
simple bridging between the value producer and the value consumer: The producer submits the new 
value to the `Receiver` side of a `MutableRepository`, and the consumer observes and retrieves the 
value via the `Repository` side.

### Compiling Repositories

In combining the functional behavior and event notification system, the `Repository` is the 
most useful component in Agera. As with `Function`s Agera provides a compiler to make it 
easier to write complex repositories. The repository compiler has instructions to separately 
specify _what_ to do, _when_ to do it, and _where_ to do it:

* _What_ is specified using the Agera functional interfaces
* _When_ is using the Agera asynchronous event notification interfaces
* _Where_ is using the Java `Executor` interface

The following is a simple example of how to use the repository compiler to create a `Repository`:

    Repository<String> repository =
        repositoryWithInitialValue("") //Initial value returned by get() before the supplier is done
            .observe(observable) // An observable event source for this repository to trigger on
            .onUpdatesPerLoop() // Update on each Looper "loop", instead of filtering per ms.
            .goTo(executor) // Put the repository flow on the given executor
            .thenGetFrom(supplier) // Gets data on the executor adding the result to the repository
            .compile(); // Returns the compiled repository

The example makes use of:

* An event source, _when_, implementing the `Observable` interface
* A data source, _what_, implementing the `Supplier` interface
* An `Executor`, _where_

Typically the `Supplier` would get the data using slow IO such as a database, networking etc.

When reusing an existing `Supplier` the data returned might be of a different type than needed in 
the `Repository`. The repository compiler provides ways to transform the data returned from the 
supplier:

    Repository<String> repository =
        repositoryWithInitialValue("Initial String")
            .observe(observable)
            .onUpdatesPerLoop()
            .goTo(executor)
            .thenGetFrom(supplier)
            .transform(transformer) // Function transforming the value to a new type
            .compile();
   
Of course the `Function` passed to the instruction `transformer` can be created using the function 
compiler. 

The `Repository` above can in turn be reused to create another `Repository`:

    Repository<Integer> secondRepository =
        repositoryWithInitialValue(0)
            .observe(repository)
            .onUpdatesPerLoop()
            .goTo(executor)
            .thenGetFrom(repository)
            .transform(transformToInteger)
            .compile();

The repository compiler has lots of more capabilities than this, including support for interrupting 
incomplete functions on updates or termination (removal of last `Updatable`), sending intermediate 
values (or reports) mid-flow to `Receiver`s, merging in data from additional sources of data, early 
termination etc. `RepositoryCompilerStates` documents the full repository compiler interface.

## Implementing a custom `Observable` service

Agera provides everything needed to help implementing an `Observable`. 
`Observables.updateDispatcher()` returns an asynchronous implementation of `UpdateDispatcher`, an 
interface extending both `Observable` and `Updatable`. When `UpdateDispatcher.update()` is called 
on this asynchronous instance, each added `Updatable` will be notified, __on the thread it was 
added on__. `UpdateDispatcher.update()` can thus be called from any thread, and Agera will handle 
the rest (For this to work, `UpdateDispatcher.addUpdatable(Updatable)` has to be called from a 
`Looper` thread, if not an exception will be thrown).

In the example below `MyObservableService` is implemented with _composition_ using the asynchronous 
`UpdateDispatcher` provided by `Observables.updateDispatcher()`:

    public final class MyObservableService implements Observable {
      private final UpdateDispatcher updateDispatcher;

      public MyObservableService() {
        this.updateDispatcher = Observables.updateDispatcher();
      }

      @Override
      public void addUpdatable(Updatable updatable) {
        updateDispatcher.addUpdatable(updatable);
      }

      @Override
      public void removeUpdatable(Updatable updatable) {
        updateDispatcher.removeUpdatable(updatable);
      }

      @Override
      private void calledAsAResultFromSomeServiceCalculation() {
        updateDispatcher.update(); //Notifies all client Updatables on their calling thread
      }
    }

The service in the example can now make requests in turn, possibly on other threads, and call 
`UpdateDispatcher.update()` when it is done. It can even call this from any thread it wants, such 
as from a thread in an `Executor`. The clients are guaranteed to be called back on their own 
threads.

`MyObservableService` will most likely in turn listen to other `Observables` to get notified when 
the service calculation should be done. Since every add/remove to an `Observable` needs to be 
balanced, an `Observable` implementation needs a way to know when to add/remove. This is where 
where Agera's _suspendable_ support comes in.

An `Observable` is in one of two states, _active_ or _suspended_. `MyObservableService` in the 
previous example is always _active_ (although, exactly where it gets its data from is not 
specified). Strictly speaking, an `Observable` never needs to be active if there is no one 
interested in its notifications. The asynchronous `UpdateDispatcher` not only provides asynchronous 
communication with clients, but also makes `Observable` services _suspendable_, if created with 
`Observables.updateDispatcher(UpdatablesChanged)`.

`UpdatablesChanged` has two methods:

* `UpdatablesChanged.firstUpdatableAdded(UpdateDispatcher)` - Called when the first `Updatable`
    is added to the `UpdateDispatcher`
* `UpdatablesChanged.lastUpdatableRemoved(UpdateDispatcher)` - Called when the last `Updatable`
    is removed from the `UpdateDispatcher`

The following example shows an `Observable` service that is updated based on a source `Observable`:

    public final class MySecondObservableService implements Observable, Updatable,
        UpdatablesChanged {
      private final UpdateDispatcher updateDispatcher;
      private final Observable sourceObservable;

      public MySecondObservableService(Observable sourceObservable) {
        this.updateDispatcher = Observables.updateDispatcher(this);
        this.sourceObservable = sourceObservable;
      }

      @Override
      public void firstUpdatableAdded(UpdateDispatcher updateDispatcher) {
        sourceObservable.addUpdatable(this);
        update(); //Explicitly call update, with no updatables this service is likely to be stale
      }

      @Override
      public void lastUpdatableRemoved(UpdateDispatcher updateDispatcher) {
        sourceObservable.removeUpdatable(this);
        //Optionally clean up resources not needed
      }

      @Override
      public void addUpdatable(Updatable updatable) {
        updateDispatcher.addUpdatable(updatable);
      }

      @Override
      public void removeUpdatable(Updatable updatable) {
        updateDispatcher.removeUpdatable(updatable);
      }

      @Override
      public void update() {
        //Service updated
        //Calculate some value in the service
        updateDispatcher.update(); //Notifies all client Updatables on their calling thread
      }
    }

This provides the `Observable` with a life cycle, _active_ when `Updatables` are added, and 
_suspended_ when not.

This life cycle will migrate down through the hierarchy of `Observables`, managing suspending 
unused services as the `Activity` goes through the `Activity` life cycle.

Note that the `Observable` service in the example explicitly calls `Updatable.update()` when the 
first `Updatable` is added. Since the service has been _suspended_ any value calculated by the 
service is likely to be stale and needs to be refreshed.

As the last `Updatable` is removed, the service has an opportunity to clean out possible resources 
no longer needed when it is _suspended_. This makes for more memory efficient code.

## Implementing a custom `Repository`

Although building a `Repository` using the repository compiler is preferred it is still possible 
to write a custom implementation of the interface in the few cases this might be needed.

A  reactive `Repository` hierarchy can be built using the same mechanisms as when building an 
`Observable`.

    public final class SumRepository implements Repository<Integer> {
      private final UpdateDispatcher updateDispatcher;
      private final Repository<Integer> first;
      private final Repository<Integer> second;

      public SumRepository(Repository<Integer> first, Repository<Integer> second) {
        this.updateDispatcher = Observables.updateDispatcher(this);
        this.first = first;
        this.second = second;
      }

      @Override
      public void firstUpdatableAdded(UpdateDispatcher updateDispatcher) {
        first.addUpdatable(updateDispatcher);
        second.addUpdatable(updateDispatcher);
      }

      @Override
      public void lastUpdatableRemoved(UpdateDispatcher updateDispatcher) {
        first.removeUpdatable(updateDispatcher);
        second.removeUpdatable(updateDispatcher);
      }

      @Override
      public void addUpdatable(Updatable updatable) {
        updateDispatcher.addUpdatable(updatable);
      }

      @Override
      public void removeUpdatable(Updatable updatable) {
        updateDispatcher.removeUpdatable(updatable);
      }

      @Override
      public Integer get() {
        return first.get() + second.get();
      }
    }

In the example, the `SumRepository` calculates its value on demand, rather than when `first` and 
`second` are updated. When doing so, the service doesn't need a state of its own.

Note that although the service is not implementing `Updatable` itself, it still uses its own 
`UpdateDispatcher`. This is done since it is not allowed to pass on an `Updatable` to an 
`Observable` created by someone else. There is no guarantee that the creator of the 
`SumRepository` isn't adding the client `Updatable` to the injected `Observables` (perhaps as a 
part of a greater more complex algorithm). Using an intermediate `UpdateDispatcher` guarantees that 
the `Updatable` is only added to the `Observable` once.

It is not always possible to calculate a value on demand. Especially if the value needs to be 
fetched from slow IO (remember, __supplying values to clients needs to be synchronous and fast__) 
the fetched value needs to be stored within the `Repository`. In the following example the 
implemented `Repository` stores its value using the synchronous `MutableRepository`.

    public final class StringRepository implements Repository<String>, Updatable,
        StringCallback {
      private final MutableRepository<String> stringRepository;
      private final Observable eventSource;
      private final AsyncStringFetcher stringFetcher;

      private StringRepository(Observable eventSource) {
        this.stringRepository = Repositories.mutableRepository("");
        this.eventSource = eventSource; //Triggered whenever a new value should be fetched
        this.stringFetcher = new StringFetcher();
      }

      public static Repository<String> stringRepository(Observable eventSource) {
        return new StringRepository(eventSource);
      }

      @Override
      public void firstUpdatableAdded(UpdateDispatcher updateDispatcher) {
        eventSource.addUpdatable(this);
        update();
      }

      @Override
      public void lastUpdatableRemoved(UpdateDispatcher updateDispatcher) {
        eventSource.removeUpdatable(this);
      }

      @Override
      public void addUpdatable(Updatable updatable) {
        stringRepository.addUpdatable(updatable);
      }

      @Override
      public void removeUpdatable(Updatable updatable) {
        stringRepository.removeUpdatable(updatable);
      }

      @Override
      public String get() {
        return stringRepository.get();
      }

      @Override
      public void update() {
        stringFetcher.fetchString(this); //Using this as callback
      }

      @Override
      public void onStringFetched(String string) {
         stringRepository.accept(string);
      }
    }

As always, it is not allowed to add an `Updatable` to an `Observable` not owned. The 
`stringRepository` is however owned by this service and implements `Observable`, so these calls can 
be directly routed to `stringRepository`. As `stringRepository` gets a new value in 
`onStringFetched` the clients are notified.

The `AsyncStringFetcher` in the example might very well call `onStringFetched` from another thread. 
Since `stringRepository` is an instance of the asynchronous `MutableRepository` the value is 
synchronized and the clients called on their own threads.
