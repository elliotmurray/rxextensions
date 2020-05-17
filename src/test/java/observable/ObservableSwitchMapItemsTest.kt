package observable

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.co.elliotmurray.rxextensions.observable.ObservableSwitchMapItems
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.elliotmurray.rxextensions.assertLastValue

class ObservableSwitchMapItemsTest {

    @Test
    fun observableSwitchMapItemsTest() {
        val source = PublishSubject.create<List<Int>>()


        val testSub = ObservableSwitchMapItems<Int, String>(source, {
            Observable.just(it.toString())
        }).test()

        testSub.assertEmpty()

        source.onNext(listOf(1, 2, 3))

        testSub.assertValues(listOf("1", "2", "3"))

        source.onNext(listOf(1, 2))

        testSub.assertValues(listOf("1", "2", "3"), listOf("1", "2"))

        source.onNext(listOf(1, 2, 4))

        testSub.assertValues(listOf("1", "2", "3"), listOf("1", "2"), listOf("1", "2", "4"))
    }

}