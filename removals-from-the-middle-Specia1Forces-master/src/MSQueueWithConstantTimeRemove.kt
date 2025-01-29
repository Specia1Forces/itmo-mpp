@file:Suppress("DuplicatedCode", "FoldInitializerAndIfToElvis")

import java.util.concurrent.atomic.*

class MSQueueWithConstantTimeRemove<E> : QueueWithRemove<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(element = null, prev = null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        // TODO: When adding a new node, check whether
        // TODO: the previous tail is logically removed.
        // TODO: If so, remove it physically from the linked list.
        //TODO("Implement me!")


        // При добавлении нового узла проверьте, является ли
        // предыдущий хвост логически удален.
        // Если это так, физически удалите его из связанного списка.

        while (true) {
            val curTail = tail.get()
            val node = Node(element = element, prev = curTail)
            /*
            if (curTail.extractedOrRemoved) {//может падать ЛИБО завернуть в while
                val prev = curTail.prev.get()
                if (prev != null) {
                    prev.next.compareAndSet(tail.get(),null)
                }
                tail.compareAndSet(curTail, prev)
                curTail = tail.get()
            }

             */
            if (curTail.next.compareAndSet(null, node)) {
                tail.compareAndSet(curTail, node)//обновили хвост
                if (curTail.extractedOrRemoved) {
                    curTail.remove()
                }

                return
            } else {
                tail.compareAndSet(curTail, curTail.next.get())
            }
        }

    }

    override fun dequeue(): E? {
        // TODO: After moving the `head` pointer forward,
        // TODO: mark the node that contains the extracting
        // TODO: element as "extracted or removed", restarting
        // TODO: the operation if this node has already been removed.
        //TODO("Implement me!")
        // После перемещения указателя `head` вперед
        // пометьте узел, содержащий извлекаемый элемент
        //, как "извлеченный или удаленный", перезапустив операцию
        //, если этот узел уже был удален.
        while (true) {
            val curHead = head.get()
            val curHeadNext = curHead.next.get()
            if (curHeadNext == null) {
                return null
            }
            if (head.compareAndSet(curHead, curHeadNext)) {
                curHeadNext.prev.set(null)
                if (curHeadNext.markExtractedOrRemoved()) {
                    val temp = curHeadNext.element
                    curHeadNext.element = null
                    return temp
                }
            }
        }


    }

    override fun remove(element: E): Boolean {
        // Traverse the linked list, searching the specified
        // element. Try to remove the corresponding node if found.
        // Пройдитесь по связанному списку, ища указанный
        // элемент. Попробуйте удалить соответствующий узел, если он будет найден.
        // DO NOT CHANGE THIS CODE.
        var node = head.get()
        while (true) {
            val next = node.next.get()
            if (next == null) return false
            node = next
            if (node.element == element && node.remove()) return true
        }
    }

    /**
     * This is an internal function for tests.
     * DO NOT CHANGE THIS CODE.
     */
    override fun validate() {
        check(head.get().prev.get() == null) {
            "`head.prev` must be null"
        }
        check(tail.get().next.get() == null) {
            "tail.next must be null"
        }
        // Traverse the linked list
        var node = head.get()
        while (true) {
            if (node !== head.get() && node !== tail.get()) {
                check(!node.extractedOrRemoved) {
                    "Removed node with element ${node.element} found in the middle of the queue"
                }
            }
            val nodeNext = node.next.get()
            // Is this the end of the linked list?
            if (nodeNext == null) break
            // Is next.prev points to the current node?
            val nodeNextPrev = nodeNext.prev.get()
            check(nodeNextPrev != null) {
                "The `prev` pointer of node with element ${nodeNext.element} is `null`, while the node is in the middle of the queue"
            }
            check(nodeNextPrev == node) {
                "node.next.prev != node; `node` contains ${node.element}, `node.next` contains ${nodeNext.element}"
            }
            // Process the next node.
            node = nodeNext
        }
    }

    private class Node<E>(
        var element: E?,
        prev: Node<E>?
    ) {
        val next = AtomicReference<Node<E>?>(null)
        val prev = AtomicReference(prev)

        /**
         * TODO: Both [dequeue] and [remove] should mark
         * TODO: nodes as "extracted or removed".
         */
        /* Как в [удалить из очереди], так и в [удалить] заметки должны
        *  быть помечены как "извлеченные или удаленные".
        */
        private val _extractedOrRemoved = AtomicBoolean(false)
        val extractedOrRemoved
            get() =
                _extractedOrRemoved.get()

        fun markExtractedOrRemoved(): Boolean =
            _extractedOrRemoved.compareAndSet(false, true)

        /**
         * Removes this node from the queue structure.
         * Returns `true` if this node was successfully
         * removed, or `false` if it has already been
         * removed by [remove] or extracted by [dequeue].
         */
        /**
         * Удаляет этот узел из структуры очереди.
         * Возвращает значение "true", если этот узел был успешно удален
         *, или "false", если он уже был удален
         * с помощью [remove] или извлечен с помощью [dequeue].
         */
        fun remove(): Boolean {
            // TODO: As in the previous task, the removal procedure is split into two phases.
            // TODO: First, you need to mark the node as "extracted or removed".
            // TODO: On success, this node is logically removed, and the
            // TODO: operation should return `true` at the end.
            // TODO: In the second phase, the node should be removed
            // TODO: physically, updating the `next` field of the previous
            // TODO: node to `this.next.value`.
            // TODO: In this task, you have to maintain the `prev` pointer,
            // TODO: which references the previous node. Thus, the `remove()`
            // TODO: complexity becomes O(1) under no contention.
            // TODO: Do not remove physical head and tail of the linked list;
            // TODO: it is totally fine to have a bounded number of removed nodes
            // TODO: in the linked list, especially when it significantly simplifies
            // TODO: the algorithm.
            //TODO("Implement me!")

            // Как и в предыдущем задании, процедура удаления разделена на два этапа.
            // Сначала вам нужно пометить узел как "извлеченный или удаленный".
            // В случае успеха этот узел логически удаляется, и
            // операция должна вернуть значение "true" в конце.
            // На втором этапе узел должен быть удален
            // физически, обновив поле "следующий" предыдущего
            // узла до `это.следующее.значение`.
            // В этой задаче вам необходимо сохранить указатель "предыдущий",
            // который ссылается на предыдущий узел. Таким образом, "удалить()"
            // сложность становится равной O(1) без каких-либо возражений.
            // Не удаляйте физические начало и конец связанного списка;
            // вполне нормально иметь ограниченное количество удаленных узлов
            // в связанном списке, особенно если это значительно упрощает
            // алгоритм.

            //cas

            //if node == tail || head == node return removed
            /*
            var curPrev = prev
            val curNext = next
            curPrev.compareAndSet(this, curNext.get())
             */
            val removed = markExtractedOrRemoved()
            val node3 = next.get()
            val node1 = prev.get()
            if (node1 == null || node3 == null) {
                return removed
            }

            node1.next.compareAndSet(this,node3)
            node3.prev.compareAndSet(this,node1)

            if (node1.extractedOrRemoved) {
                node1.remove()
            }


            if (node3.extractedOrRemoved == true) {
                node3.remove()
            }
            return removed

        }


    }
}