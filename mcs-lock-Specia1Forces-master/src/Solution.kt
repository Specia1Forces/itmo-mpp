import java.util.concurrent.atomic.*


class Solution(private val env: Environment) : Lock<Solution.Node> {
    // todo: необходимые поля (val, используем AtomicReference)
    private val tail = AtomicReference<Node>()
    //public final val tail: AtomicReference<Node> = null


    override fun lock(): Node {

        val my = Node() // сделали узел
        //my.value.next.set(null)
        //my.value.next.get()=null
        //my.lock.set(true)
        //my.value.lock(true)
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.next.value = my
            while (my.lock.value) {
                env.park() //Environment
            }
        }
        // todo: алгоритм
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null)) {
                return
            }
            while (node.next.value == null) {
                //вечный цикл
            }

        }
        val tempNode = node.next.value
        if (tempNode != null) {
            tempNode.lock.value = false
            env.unpark(tempNode.thread)
        }
        // todo: алгоритм

    }

    class Node {

        val thread = Thread.currentThread() // запоминаем поток, которые создал узел

        // todo: необходимые поля (val, используем AtomicReference)
        val lock = AtomicReference(true)
        val next = AtomicReference<Node?>()
    }
}