import java.util.concurrent.atomic.*
import kotlin.math.*

/**
 * @author Stolbov, Dmitry
 */
class FAABasedQueueSimplified<E> : Queue<E> {
    private val infiniteArray = AtomicReferenceArray<Any?>(32) // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        // TODO: Increment the counter atomically via Fetch-and-Add. Увеличьте счетчик атомарно с помощью функции выборки и добавления.
        // TODO: Use `getAndIncrement()` function for that. Для этого используйте функцию `getAndIncrement()`.
        //val i = enqIdx.get()
        //enqIdx.set(i + 1)
        //val i = enqIdx.getAndIncrement()
        // TODO: Atomically install the element into the cell Автоматическая установка элемента в ячейку
        // TODO: if the cell is not poisoned.если клетка не отравлена.
        //infiniteArray.set(i.toInt(), element)
        while (true) {
            val i = enqIdx.getAndIncrement()
            if (infiniteArray.compareAndSet(i.toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        // Is this queue empty?
        //if (enqIdx.get() <= deqIdx.get()) return null
        // TODO: Increment the counter atomically via Fetch-and-Add. Увеличьте счетчик атомарно с помощью функции выборки и добавления.
        // TODO: Use `getAndIncrement()` function for that.Для этого используйте функцию `getAndIncrement()`.
        //val i = deqIdx.get()
        //deqIdx.set(i + 1)
        // TODO: Try to retrieve an element if the cell contains an Попробуйте извлечь элемент, если ячейка содержит
        // TODO: element, poisoning the cell if it is empty. элемент, отравляющий ячейку, если она пуста.
        //return infiniteArray.get(i.toInt()) as E

        while (true) {
            if (deqIdx.get() >= enqIdx.get()) return null // либо  (enqIdx.get() <= deqIdx.get()) return null  //(deqIdx.get() >= enqIdx.get())
            val i = deqIdx.getAndIncrement()
            if (infiniteArray.compareAndSet(i.toInt(), null, POISONED)) {
                continue
            }

            return infiniteArray.getAndSet(i.toInt(),null) as E
        }
    }

    override fun validate() {
        for (i in 0 until min(deqIdx.get().toInt(), enqIdx.get().toInt())) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `deqIdx = ${deqIdx.get()}` at the end of the execution"
            }
        }
        for (i in max(deqIdx.get().toInt(), enqIdx.get().toInt()) until infiniteArray.length()) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `enqIdx = ${enqIdx.get()}` at the end of the execution"
            }
        }
    }
}

// TODO: poison cells with this value. отравляйте клетки этим значением.
private val POISONED = Any()
