package uk.co.elliotmurray.rxextensions

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subscribers.TestSubscriber
import uk.co.elliotmurray.rxextensions.observable.ObservableSwitchMapItems
import java.util.concurrent.TimeUnit
import kotlin.math.exp

fun <T> Observable<Nullable<T>>.filterNotNull(): Observable<T> {
    return mapNotNull {
        it.data
    }
}

fun <T, O> Observable<T>.mapNotNull(func: (t: T) -> O?): Observable<O> {
    return concatMap {
        val toReturn = func(it)
        if (toReturn == null) {
            Observable.empty()
        } else {
            Observable.just(toReturn)
        }
    }
}

fun <T> Observable<T>.scanToSet(): Observable<Set<T>> {
    return scan(kotlin.collections.mutableSetOf<T>(), { set, item ->
        set.add(item)
        set
    })
        .map {
            it as Set<T>
        }
}

@JvmName("addToSet")
fun <T> BehaviorSubject<Set<T>>.add(item: T) {
    onNext((value ?: kotlin.collections.emptySet()) + item)
}

@JvmName("addToList")
fun <T> BehaviorSubject<List<T>>.add(item: T) {
    onNext((value ?: kotlin.collections.emptyList()) + item)
}

fun <T, O> Observable<List<T>>.mapList(func: (t: T) -> O): Observable<List<O>> {
    return map { list -> list.map { item -> func(item) } }
}

fun <T1, T2, O> Observable<T1>.combineLatest(other: Observable<T2>, func: (t1: T1, t2: T2) -> O): Observable<O> {
    return Observable.combineLatest(this, other, BiFunction { t1, t2 -> func(t1, t2) })
}

fun <T1, T2> Observable<T1>.combineLatestPair(other: Observable<T2>): Observable<Pair<T1, T2>> {
    return Observable.combineLatest(this, other, BiFunction { t1, t2 -> t1 to t2 })
}

fun <T1, T2> Observable<T1>.withLatestFromPair(other: Observable<T2>): Observable<Pair<T1, T2>> {
    return this.withLatestFrom(other, BiFunction { t1, t2 -> t1 to t2 })
}

fun <T1, T2, T3> Observable<T1>.combineLatestTriple(other: Observable<T2>): Observable<Triple<T1, T2, T3>> {
    return Observable.combineLatest(this, other, BiFunction { t1, t2, t3 -> Triple(t1, t2, t3) })
}

fun <T1, T2, T3> Observable<T1>.withLatestFromTriple(other: Observable<T2>): Observable<Triple<T1, T2, T3>> {
    return this.withLatestFrom(other, BiFunction { t1, t2, t3 -> Triple(t1, t2, t3) })
}

fun <T1> Observable<T1>.takeWhen(other: Observable<*>): Observable<T1> {
    return other.withLatestFromPair(this)
        .map { it.second }
}

fun <T> List<Observable<T>>.combineLatest(): Observable<List<T>> = Observable.combineLatest(this) {
    @Suppress("UNCHECKED_CAST")
    (it as Array<out T>).toList()
}

fun <T> Observable<T>.print(mapper: (T) -> String = {it.toString()}): Observable<T> = doOnNext {
    println(mapper(it))
}

fun <T> Observable<T>.print(tag: String, mapper: (T) -> String = {it.toString()}): Observable<T> = doOnNext {
    println("$tag: ${mapper(it)}")
}

/**
 * Throttles based on [other] and [time], if [other] fires then the source observable wont fire again
 * until [time] has expired. After this point the source observable can freely fire until the next
 * [other] event.
 */
fun <T> Observable<T>.throttleLatestWith(
        other: Observable<*>,
        time: Long,
        timeUnit: TimeUnit
): Observable<T> {
    return combineLatestPair(
            other.switchMap {
                Observable.merge(
                        Observable.just(false),
                        Observable.timer(time, timeUnit).map { true }
                )
            }
                    .startWithItem(true)
    )
            .filter {
                it.second
            }
            .map { it.first }
            .throttleLatest(time, timeUnit)
}

fun <T> Observable<T>.doOnNext(func: (t: T) -> Any?) = this.map {
    func(it)
    it
}

fun <T> Observable<T>.doOnNextConcat(func: (t: T) -> Completable) = this.concatMap {
    func(it)
            .toSingleDefault(it)
            .toObservable()
}

//Testing
fun <T> TestObserver<T>.assertLastValue(expected: T): TestObserver<T> = assertValueAt(values().size - 1, expected)
fun <T> TestObserver<T>.assertLastValue(expectedPredicate: (T) -> Boolean): TestObserver<T> = assertValueAt(values().size - 1, expectedPredicate)


fun <T, R : Any> Observable<List<T>>.switchMapItems(mapper: (T) -> Observable<R>): Observable<List<R>> = ObservableSwitchMapItems(this, mapper)
fun <T, R : Any> Observable<List<T>>.switchMapItems(defaultValue: (T) -> R, mapper: (T) -> Observable<R>): Observable<List<R>> = ObservableSwitchMapItems(this, mapper, defaultValue)
fun <T, R : Any> Observable<List<T>>.switchMapItems(defaultValue: R, mapper: (T) -> Observable<R>): Observable<List<R>> = ObservableSwitchMapItems(this, mapper, { defaultValue })