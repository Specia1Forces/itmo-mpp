import java.util.concurrent.atomic.*


/**
 * @author TODO: Stolbov, Dmitry
 */
class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {

        while (true) {
            val node = Node<E>(element)
            val curTail = tail.get()

            if (curTail.next.compareAndSet(null, node)) { // tail.compareAndSet(null,node)
                tail.compareAndSet(curTail, node)//обновили хвост
                //tail.compareAndSet(node, null) //доработать с tail работать
                return
            } else {
                tail.compareAndSet(curTail, curTail.next.get())
            }
        }

    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val curHeadNext = curHead.next.get()
            if (curHeadNext == null) {
                return null
            }
            if (head.compareAndSet(curHead, curHeadNext)) { //это походу надо работать head
                val temp = curHeadNext.element //cas getAndSet
                curHeadNext.element = null
                return temp
                //return curHeadNext.element
            }
        }
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "At the end of the execution, `tail.next` must be `null`" //"В конце выполнения `tail.next` должно быть `null`"
        }
        check(head.get().element == null) {
            "At the end of the execution, the dummy node shouldn't store an element" //"В конце выполнения фиктивный узел не должен сохранять элемент"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
