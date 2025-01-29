package mpp.dynamicarray

import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicBoolean

interface DynamicArray<E> {
    /**
     * Returns the element located in the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun get(index: Int): E

    /**
     * Puts the specified [element] into the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun put(index: Int, element: E)

    /**
     * Adds the specified [element] to this array
     * increasing its [size].
     */
    fun pushBack(element: E)

    /**
     * Returns the current size of this array,
     * it increases with [pushBack] invocations.
     */
    val size: Int
}

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<Cell<E>>(INITIAL_CAPACITY))
    private val moveBoolean = atomic(false)


    override fun get(index: Int): E {
        return getElement(index).value
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val temp = getElement(index)
            if (temp is MovableCell<E>) {
                continue
            }
            if (core.value.casCell(index, temp, Cell(element))) {
                return
            }

        }

    }

    override fun pushBack(element: E) {
        while (true) {
            val curCore = core.value
            val localSize = size
            val cap = curCore.getCapacity()
            if (localSize >= cap) {
                if (moveBoolean.compareAndSet(false, true)) {
                    resize(cap)
                    continue
                } else {
                    continue
                }
            } else {
                if (curCore.casCell(localSize, null, Cell(element))) {
                    curCore.incrementSize(localSize)//может быть проблемка
                    return
                }
                curCore.incrementSize(localSize)
            }
        }
    }

    private fun resize(cap: Int) {

        val oldCapacity = core.value.getCapacity()
        val newCapacity = oldCapacity * 2
        val newCore = Core<Cell<E>>(newCapacity)
        newCore.compareAndSetSize(0, oldCapacity)
        val curCore = core
        /*
        if (curCore.value.next.compareAndSet(null, newCore)) {
            return
        }

         */
        //next.compareAndSet(atomic<Core<E>?>(null).value, newCore.value)//break
        //цикл for по копированию

        for (i in 0..<cap) {
            var temp = curCore.value.get(i)

            if (temp == null) {
                continue
            }

            while (true) {
                val movableCell = MovableCell(temp)
                if (curCore.value.casCell(i, temp, movableCell.value)) {
                    newCore.casCell(i, null, temp)
                    break
                }
                temp = curCore.value.get(i)
                if (temp == null) {
                    break
                }
            }

        }
        core.compareAndSet(curCore.value, newCore)
        moveBoolean.compareAndSet(true, false)
        //return core.compareAndSet(curCore.value, newCore)
    }

    private fun getElement(idx: Int): Cell<E> {
        return core.value.get(idx) ?: throw IllegalArgumentException()
    }

    override val size: Int get() = core.value.getSize()
}

class Core<E>(
    capacity: Int,
) {
    private val array = atomicArrayOfNulls<E>(capacity)
    private val _size = atomic(0)

    //private val next = atomic<Core<E>?>(null)
    val next = atomic<Core<E>?>(null)
    private val size: Int = _size.value


    fun get(index: Int): E? {
        require(index < size)
        return array[index].value
    }


    fun casCell(index: Int, expected: E?, updated: E?): Boolean {
        require(index < getCapacity())
        return array[index].compareAndSet(expected, updated)
    }

    fun getCapacity(): Int {
        return array.size
    }

    fun incrementSize(value: Int) {
        while (true) {
            if(_size.compareAndSet(value, value + 1)){
                break
            }
        }
    }

    fun getSize(): Int {
        return size
    }

    fun compareAndSetSize(exp: Int, new: Int): Boolean {
        return _size.compareAndSet(exp, new)
    }


}

private open class Cell<E>(val value: E)
private class MovableCell<E>(value: E) : Cell<E>(value)

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME

