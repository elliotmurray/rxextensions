package observable

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.co.elliotmurray.rxextensions.observable.ObservableSwitchMapItems
import io.reactivex.rxjava3.core.Observable

class ObservableSwitchMapItemsTest {

    @Test
    fun test() {
        //HMMMMMMMM
        println(ObservableSwitchMapItems<Int, String>(Observable.just(listOf(1, 2, 3)), {
            Observable.just(it.toString())
        }).test().values())
//                .assertValue(listOf("1", "2", "3"))


    }

}