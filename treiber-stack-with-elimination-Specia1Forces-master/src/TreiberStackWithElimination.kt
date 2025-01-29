import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author TODO: Stolbov, Dmitry
 */
open class TreiberStackWithElimination<E : Any> : Stack<E> {
    private val stack = TreiberStack<E>()

    // TODO: Try to optimize concurrent push and pop operations, Попытаться оптимизировать параллельные push- и pop-операции,
    // TODO: synchronizing them in an `eliminationArray` cell. синхронизировать их в ячейке "исключающий массив".
    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        //TODO("Implement me!")
        // TODO: Choose a random cell in `eliminationArray` ЧТО НУЖНО СДЕЛАТЬ: Выбрать случайную ячейку в "исключающем массиве"
        // TODO: and try to install the element there.  и попытаться установить элемент в нее.
        // TODO: Wait `ELIMINATION_WAIT_CYCLES` loop cycles дождаться цикла ELIMINATION_WAIT_CYCLES
        // TODO: in hope that a concurrent `pop()` grabs the ЧТО НУЖНО СДЕЛАТЬ: в надежде, что параллельная функция `pop()` захватит элемент
        // TODO: element. If so, clean the cell and finish, Если это так, очистите ячейку и завершите,
        // TODO: returning `true`. Otherwise, move the cell  вернет значение `true'. В противном случае переместите ячейку
        // TODO: to the empty state and return `false`.  перевести в пустое состояние и вернуть `false`.
        val randomCellInd = randomCellIndex()
        if (eliminationArray.compareAndSet(randomCellInd, CELL_STATE_EMPTY, element)) {
            var i = 0
            while (i < ELIMINATION_WAIT_CYCLES) {//либо <
                i++
                if (eliminationArray.compareAndSet(randomCellInd, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                    return true
                }
            }

                if (eliminationArray.compareAndSet(randomCellInd, element, CELL_STATE_EMPTY)) {
                    return false
                }
                if (eliminationArray.compareAndSet(randomCellInd, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                    return true
                }

           
        }
        return false
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        //TODO("Implement me!")
        // TODO: Choose a random cell in `eliminationArray` Выберите случайную ячейку в `исключающем массиве`
        // TODO: and try to retrieve an element from there. и попытайтесь извлечь оттуда элемент.
        // TODO: On success, return the element. В случае успеха верните элемент.
        // TODO: Otherwise, if the cell is empty, return `null`. В противном случае, если ячейка пуста, верните значение `null`.

        //var randomElement = eliminationArray.get(randomCellIndex()) //Выбрали случайный элемент и извлекли
        var i = 0
        while (i < ELIMINATION_WAIT_CYCLES) {//либо <
            i++
            //val done = "Done"
            val randomCellIndex = randomCellIndex()
            val randomElement: Any? = eliminationArray.get(randomCellIndex)

            if (randomElement != CELL_STATE_EMPTY&& randomElement != CELL_STATE_RETRIEVED && eliminationArray.compareAndSet(randomCellIndex, randomElement, CELL_STATE_RETRIEVED)) {
                @Suppress("UNCHECKED_CAST")
                return randomElement as E//?
            }
        }
        return CELL_STATE_EMPTY
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state. Изначально все ячейки находятся в пустом состоянии.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state изменяет состояние ячейки
        // to `RETRIEVED` if the cell contains element. чтобы "ИЗВЛЕЧЬ`, если ячейка содержит элемент.
        private val CELL_STATE_RETRIEVED = Any()
    }
}
