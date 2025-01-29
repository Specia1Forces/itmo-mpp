import java.util.concurrent.atomic.*

/**
 * @author Stolbov, Dmitry
 *
 * TODO: Copy the code from `FAABasedQueueSimplified`
 * TODO: and implement the infinite array on a linked list
 * TODO: of fixed-size `Segment`s.
 */
/*
Скопируйте код из `Упрощенной очереди на основе FAA"
и реализуйте бесконечный массив в связанном списке
сегментов фиксированного размера.
 */

class FAABasedQueue<E> : Queue<E> {

    private var head = AtomicReference(Segment(0))
    private var tail = AtomicReference(head.get()) //head

    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)


    override fun enqueue(element: E) {
        while (true) {
            //val curTail = tail
            val curTail = tail.get()
            val i = enqIdx.getAndIncrement().toInt()
            val segment = findSegment(curTail, i / SEGMENT_SIZE)
            moveTailForward(segment)
            if (segment.compareAndSet((i % SEGMENT_SIZE).toInt(), null, element) == true) {
                return
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            //if (!shouldTryToDequeue()) return null
            if (deqIdx.get() >= enqIdx.get()) return null
            val curHead = head.get()
            val i = deqIdx.getAndIncrement().toInt()
            val segment = findSegment(curHead, i / SEGMENT_SIZE)
            moveHeadForward(segment)
            if (segment.compareAndSet((i % SEGMENT_SIZE).toInt(), null, POISONED) == true) {
                continue
            }
            return segment.getAndSet((i % SEGMENT_SIZE).toInt(), null) as E
        }
    }

    private fun findSegment(start: Segment, id: Int): Segment {
        var currentSegment = start
        while (currentSegment.id < id) {
            // Проверяем, есть ли следующий сегмент
            val nextSegment = currentSegment.next.get()
            if (nextSegment != null) {
                currentSegment = nextSegment
            } else {
                // Если следующего сегмента нет, создаем новый
                currentSegment.next.compareAndSet(null, Segment(currentSegment.id + 1))
            }
        }
        // Возвращаем сегмент, когда его id становится равным или больше искомого
        return currentSegment
    }

    private fun moveTailForward(start: Segment) {
        val segmentTail = tail.get()

        if (start.id > segmentTail.id) {
            tail.compareAndSet(segmentTail, start)
        }

    }

    private fun moveHeadForward(start: Segment) {
        val segmentHead = head.get()
        if (start.id > segmentHead.id) {
            head.compareAndSet(segmentHead, start)
        }
    }

    fun shouldTryToDequeue(): Boolean {
        while (true) {
            val curEnqIdx = enqIdx.get()
            val curDeqIdx = deqIdx.get()
            if (curEnqIdx != curEnqIdx) {
                continue
            }
            return curDeqIdx < curEnqIdx
        }
    }

}

private class Segment(val id: Long) {

    var next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)

    fun <E> compareAndSet(toInt: Int, nothing: Any?, element: E): Boolean {
        return cells.compareAndSet(toInt, nothing, element)
    }

    fun getAndSet(toInt: Int, nothing: Nothing?): Any? {
        return cells.getAndSet(toInt, nothing)
    }


}


// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2

private val POISONED = Any()
