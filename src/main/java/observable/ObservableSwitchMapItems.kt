package uk.co.elliotmurray.rxextensions.observable

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

class ObservableSwitchMapItems<T, R : Any>(val source: Observable<List<T>>, val mapper: (T) -> Observable<R>, val defaultValue: (T) -> R? = {null}): Observable<List<R>>() {
    override fun subscribeActual(observer: Observer<in List<R>>) {
        source.subscribe(SwitchMapItemsObserver(observer, mapper, defaultValue))
    }

    class SwitchMapItemsObserver<T, R : Any>(
        private val observer: Observer<in List<R>>,
        private val mapper: (T) -> Observable<R>,
        private val defaultValue: (T) -> R?

    ): Observer<List<T>>,
        @NonNull Disposable {

        private lateinit var upstreamDisposable: Disposable
        var cancelled = false

        private val mappedDisposables = mutableMapOf<T, Disposable>()
        private val values = mutableMapOf<T, R>()

        private val sourceItems = mutableListOf<T>()

        private val outputItems: List<R>?
        get() = sourceItems.map {
            values[it] ?: defaultValue(it)
        }.let { list ->
            if (list.any { it == null }) {
                null
            } else {
                list.filterNotNull()
            }
        }

        private fun updateItems() {
            outputItems?.let { observer.onNext(it) }
        }

        override fun isDisposed(): Boolean = cancelled

        override fun dispose() {
            cancelled = true
            mappedDisposables.values.forEach { it.dispose() }
            mappedDisposables.clear()
            upstreamDisposable.dispose()
        }

        override fun onComplete() {
            observer.onComplete()
        }

        override fun onSubscribe(d: Disposable) {
            upstreamDisposable = d

            observer.onSubscribe(this)
        }

        override fun onNext(newKeys: List<T>) {
            val currentKeys = mappedDisposables.keys
            val same = currentKeys.intersect(newKeys)
            val added = newKeys - same
            val removed = currentKeys - same

            added.forEach { key ->
                mappedDisposables[key] = mapper(key).subscribe { value ->
                    values[key] = value
                    updateItems()
                }
            }

            removed.forEach { key ->
                mappedDisposables.remove(key)?.dispose()
            }

            updateItems()
        }

        override fun onError(e: Throwable) {
            observer.onError(e)
        }
    }
}