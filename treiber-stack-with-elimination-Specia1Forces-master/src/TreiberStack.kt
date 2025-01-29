import java.util.concurrent.atomic.AtomicReference

/**
 * @author TODO: Stolbov, Dmitry
 */
class TreiberStack<E> : Stack<E> {
    // Initially, the stack is empty.
    private val top = AtomicReference<Node<E>?>(null)

    override fun push(element: E) {
        // TODO: Make me linearizable!  Сделайте меня линеаризуемым
        // TODO: Update `top` via Compare-and-Set, Обновите "top" с помощью Compare-and-Set,
        // TODO: restarting the operation on CAS failure. перезапустите операцию при сбое CAS.
        //val curTop = top.get()
        // val newTop = Node(element, curTop)
        // top.set(newTop)
        while (true) {
            val curTop = top.get()
            val newTop = Node(element, curTop)
            if (top.compareAndSet(curTop, newTop)) {
                return
            }
        }
    }

    override fun pop(): E? {
        // TODO: Make me linearizable! Сделайте меня линеаризуемым
        // TODO: Update `top` via Compare-and-Set,       Обновите "top" с помощью Compare-and-Set
        // TODO: restarting the operation on CAS failure. перезапустите операцию при сбое CAS.
        //val curTop = top.get() ?: return null
        //top.set(curTop.next)
        //return curTop.element
        while (true) {
            val curTop = top.get()
            if (curTop == null){
                return null
            }
            val newTop = curTop.next
            if (top.compareAndSet(curTop,newTop)){
                return curTop.element
            }

        }
    }

    private class Node<E>(
        val element: E,
        val next: Node<E>?
    )
}
