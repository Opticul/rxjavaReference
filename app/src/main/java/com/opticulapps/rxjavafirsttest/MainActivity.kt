package com.opticulapps.rxjavafirsttest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.processors.AsyncProcessor
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.processors.ReplayProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val disposables = CompositeDisposable()

        Observable.intervalRange(
            10L,
            5L,
            0L,
            10L,
            TimeUnit.SECONDS
        ).subscribe { println("${it.dec()} was emitted!") }

        Observable.just("Apple", "Orange", "Banana", "On IO Thread!")
            .subscribeOn(Schedulers.io())
            .subscribe(
                { value -> println("Received: $value") },
                { error -> println("Error: $error") },
                { println("Completed!") }
            )

        Observable.just("Apple", "Orange", "Banana", "On New Thread!")
            .subscribeOn(Schedulers.newThread())
            .subscribe(
                { value -> println("Received: $value") },
                { error -> println("Error: $error") },
                { println("Completed!") }
            )

        Observable.just("Apple", "Orange", "Banana", "Switching to IO thread after new thread!")
            .subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.io()) //AndroidSchedulers.mainThread() if rxAndroid is implemented
            .subscribe(
                { value -> println("Received: $value") },
                { error -> println("Error: $error") },
                { println("Completed!") }
            )

        //Will just skip (drop) the emissions it does not have the memory to handle instead of getting an outOfMemoryException
        // Note that Flowable is exactly like Observable except it supports backpressure operators like "DROP"
        val observableThatNeedBackpressureHandling = PublishSubject.create<Int>()
        observableThatNeedBackpressureHandling
            .toFlowable(BackpressureStrategy.DROP)
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    //println("The Number Is: $it")
                },
                { t ->
                    print(t.message)
                }
            )
        for (i in 0..100) { //Replace with large number
            observableThatNeedBackpressureHandling.onNext(i)
        }

        //Returns either a single value, complete or error.
        Maybe.just("This is a Maybe")
            .subscribe(
                { value -> println("Received: $value") },
                { error -> println("Error: $error") },
                { println("Completed!") }
            )

        //Will either emit a single emission or error because it failed to do so
        Single.just("This is a Single")
            .subscribe(
                { v -> println("Value is: $v") },
                { e -> println("Error: $e") }
            )


        Completable.create { emitter ->
            emitter.onComplete()
            emitter.onError(Exception())
        }

        Observable.just("Manual", "Call", "Observable")
            .doOnSubscribe { println("Subscribed") }
            .doOnNext { s -> println("Received: $s") }
            .doAfterNext { println("After Receiving") }
            .doOnError { e -> println("Error: $e") }
            .doOnComplete { println("Complete") }
            .doFinally { println("Do Finally!") }
            .doOnDispose { println("Do on Dispose!") }
            .subscribe { println("Subscribe") }


        //Creates an observableTransformer that observe several observables
        Observable.just("One", "Two", "Three", "Four")
            .compose(applyObservableAsync())
            .subscribe { v -> println("The First Observable Received: $v") }

        Observable.just("Water", "Fire", "Earth", "Air")
            .map { m -> "† $m †" }
            .compose(applyObservableAsync())
            .subscribe { v -> println("The Second Observable Received: $v") }

        Observable.just("Water", "Fire", "Earth", "Air")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { m ->
                Observable.just(m + " flatMapped")
                    .subscribeOn(Schedulers.io())
            }
            .subscribe { v -> println("Received: $v") }

        //Takes first couple of values matched together
        Observable.zip(
            Observable.just(
                "Roses", "Sunflowers", "Leaves", "Clouds", "Violets", "Plastics"),
            Observable.just(
                "Red", "Yellow", "Green", "White or Grey", "Purple"),
            BiFunction<String, String, String> { type, color ->
                "$type are $color"
            }
        )
            .subscribe { v -> println("Received: $v") }


        val firstPart = Observable.just("Fire", "Is", "Hot!")
        val secondPart = Observable.just("Water", "Is", "Wet!")
        val thirdPart = Observable.just("Earth", "Is", "Dirty!")
        val forthPart = Observable.just("Air", "Is", "Breezy!")

    Observable.concat(firstPart,secondPart,thirdPart,forthPart) //"merge" does the same, but not in fixed order. Concat will always emit "firstpart" emissions first.
        .subscribe{println(it)}


        Observable.just(1,5,2,6,3,7,11,0)
            .filter { it > 4 } // Returns an observable with the movies below 5 removed
            .repeat(4) // Repeats all values left
            .reduce(0,{acc,x -> acc + x})
            .doAfterSuccess { println("Filter, repeat, reduce and: $it") }
            .subscribe({
                println(it)
            },{
                println("Error")
            })
        val timer = Observable.timer(5,TimeUnit.SECONDS)
        timer.blockingSubscribe() { v -> println("Block done") }

        Observable.just(1,5,2,9,6,7,11,0,33)
            .filter { it > 3 } // Returns an observable with the movies below 5 removed
            .takeUntil { it > 10 } // Pass on emissions until one is above 10
            .map{it*it} // Multiply all emissions still in the stream by itself
            .sorted() // Sort the numbers by size
            .take(2) // Take the two first emissions and ignore the rest
            .subscribe({
                println(it)
            },{
                println("Error")
            })

        val clock = Observable.interval(1,TimeUnit.SECONDS)

        val disposable = clock.subscribe{ time ->
            if (time % 10 == 0L) {
                println("$time seconds have passed!")
            }
        }
        disposables.add(disposable)
        val calledWithCallable = Observable.fromCallable{
             "A Callable Function Return!"
        }

        calledWithCallable.subscribe(
            {item -> println(item)},
            {error -> error.printStackTrace()},
            {println("Done with callable")}
        )

        // Note: Flatmap allows for interleaves, if these came at different time, the order would be different in the output/emission
        Observable.just(firstPart, secondPart, thirdPart, forthPart) //Takes in the different observables
            .flatMap {it.reduce { t1, t2 -> "$t1 $t2" }.toObservable() } // Concatinates the words into strings and return as an observable
            .subscribe { println("$it returned from flatmap") } // Print the strings, one line pr. observable

        // ConcatMap is exactly like Flatmap except 1: Things are in the order called and 2: You have to wait for it, so if the first stream is never ending the others never emit.

        val publishTest = PublishSubject.create<String>() // Creates a publisher, can be both subscribed to and added to
        publishTest.hide().subscribe{println(it)} // Makes a subscriber that prints whenever a value is emitted by the stream
        publishTest.hide().subscribe{if (it.length>5) {println(it)} }
        publishTest.onNext("Great") // Submits a value to the stream, which will be emitted by all subscribers
        publishTest.onNext("Greater!") // Submits a value with length over 5, so it is printed by both subscribers

        val processorTest = PublishProcessor.create<String>() // Same as subject above, but supports backpressure (flowable version of subject)
        processorTest.hide().subscribe{ println(it)}
        processorTest.onNext("Printed with processor!")

        val replayTest = ReplayProcessor.create<String>() // Same as publishProcessor, but stores the values so you get them all even if you subscribe after emissions
        replayTest.onNext("replayTest before subscription 1")
        replayTest.onNext("replayTest before subscription 2")
        replayTest.hide().subscribe{println(it)}
        replayTest.onNext("replayTest after subscription")
        replayTest.onComplete()

        val behaviourTest = BehaviorProcessor.create<String>() //Same as ReplayProcessor, but only return the "current" value, not all previous
        behaviourTest.onNext("behaviourTest before subscription 1")
        behaviourTest.onNext("behaviourTest before subscription 2")
        behaviourTest.hide().subscribe{println(it)}
        behaviourTest.onNext("behaviourTest after subscription")
        behaviourTest.onComplete()

        val asyncProcessorTest = AsyncProcessor.create<String>() //Same as those above, but it only returns the last value before onComplete is called, when onComplete is called
        asyncProcessorTest.hide().subscribe{println(it)}
        asyncProcessorTest.onNext("First Asynch")
        asyncProcessorTest.onNext("Second Asynch")
        asyncProcessorTest.onNext("Third Asynch")
        asyncProcessorTest.hide().subscribe{println("$it from second subscriber")}
        asyncProcessorTest.onNext("Forth Asynch")
        asyncProcessorTest.onComplete() // Both subscribers print "Forth Asynch" only, and it is being printed here.



        Observable.just(firstPart, secondPart, thirdPart, forthPart) //Takes in the different observables
            .switchMap {it.reduce { t1, t2 -> "$t1 $t2" }.toObservable()}
            .subscribe { println("$it returned from Swithmap") }

        disposables.clear() // Stops the clock (and other elements added to disposables).
    }

    fun <T> applyObservableAsync(): ObservableTransformer<T, T> {
        return ObservableTransformer { observable ->
            observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }
    // This is for "Observer", there are other types like: ObservableTransformer
    //FlowableTransformer
    //SingleTransformer
    //MaybeTransformer
    //CompletableTransformer
}