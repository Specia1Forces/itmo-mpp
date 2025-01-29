import java.util.concurrent.*
import java.util.concurrent.atomic.*

class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E) {
        // TODO: Make this code thread-safe using the flat-combining technique.
        // TODO: 1.  Try to become a combiner by
        // TODO:     changing `combinerLock` from `false` (unlocked) to `true` (locked).
        /*Попробуйте стать объединителем,
        изменив "блок объединителя" с "false" (разблокирован) на "true" (заблокирован).
        *
        * */
        // TODO: 2a. On success, apply this operation and help others by traversing
        // TODO:     `tasksForCombiner`, performing the announced operations, and
        // TODO:      updating the corresponding cells to `Result`.
        /*
         В случае успеха примените эту операцию и помогите другим, перейдя по ссылке
         "задачи для объединителя", выполнив объявленные операции и
         изменив соответствующие ячейки на `Результат`.
         */
        // TODO: 2b. If the lock is already acquired, announce this operation in
        // TODO:     `tasksForCombiner` by replacing a random cell state from
        // TODO:      `null` with the element. Wait until either the cell state
        // TODO:      updates to `Result` (do not forget to clean it in this case),
        // TODO:      or `combinerLock` becomes available to acquire.
        /*
        Если блокировка уже установлена, объявите об этой операции в
         "задачи для объединителя", заменив случайное состояние ячейки из
       "null" на элемент. Подождите, пока состояние ячейки не изменится.
       обновляется "Результат" (в этом случае не забудьте его почистить),
        или "комбинированный замок" становится доступным для приобретения.

         */
        if (combinerLock.compareAndSet(false, true)) {
            queue.addLast(element)
            for (i in 0 until TASKS_FOR_COMBINER_SIZE) {
                var task = tasksForCombiner.get(i)
                if (task != null) {
                    if (task is Dequeue) {
                        tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                    } else if (task is Result<*>) {
                        continue
                    } else { // task as E
                        queue.addLast(task as E)
                        tasksForCombiner.set(i, Result<E?>(task))
                    }
                }
            }
            combinerLock.set(false)
        } else {
            var randomCell = randomCellIndex()
            while (!tasksForCombiner.compareAndSet(randomCell, null, element)) {
                randomCell = randomCellIndex()
            }
            while (true) {
                if (tasksForCombiner.get(randomCell) is Result<*>) {
                    //уже добавили cell
                    tasksForCombiner.set(randomCell, null)
                    return
                } else if (combinerLock.compareAndSet(false, true)) {

                    val res1 = tasksForCombiner.get(randomCell)
                    tasksForCombiner.compareAndSet(randomCell, res1, null)
                    if (res1 is Result<*>) {
                        combinerLock.set(false)
                        return
                    }

                    queue.addLast(element)
                    for (i in 0 until TASKS_FOR_COMBINER_SIZE) {
                        var task = tasksForCombiner.get(i)
                        if (task != null) {
                            if (task is Dequeue) {
                                tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                            } else if (task is Result<*>) {
                                continue
                            } else { // task as E
                                queue.addLast(task as E)
                                tasksForCombiner.set(i, Result<E?>(task))
                            }
                        }
                    }
                    combinerLock.set(false)
                    return
                }
            }
        }


    }

    override fun dequeue(): E? {
        // TODO: Make this code thread-safe using the flat-combining technique.
        // TODO: 1.  Try to become a combiner by
        // TODO:     changing `combinerLock` from `false` (unlocked) to `true` (locked).
        /*
        Попробуйте стать объединителем,
        изменив "блок объединителя" с "false" (разблокирован) на "true" (заблокирован).
         */
        // TODO: 2a. On success, apply this operation and help others by traversing
        // TODO:     `tasksForCombiner`, performing the announced operations, and
        // TODO:      updating the corresponding cells to `Result`.
        /*В случае успеха примените эту операцию и помогите другим, перейдя по ссылке
        "задачи для объединителя", выполнив объявленные операции и
        изменив соответствующие ячейки на `Результат`.
         */
        // TODO: 2b. If the lock is already acquired, announce this operation in
        // TODO:     `tasksForCombiner` by replacing a random cell state from
        // TODO:      `null` with `Dequeue`. Wait until either the cell state
        // TODO:      updates to `Result` (do not forget to clean it in this case),
        // TODO:      or `combinerLock` becomes available to acquire.
        /*
        Если блокировка уже установлена, объявите об этой операции в
        "задачи для объединителя", заменив случайное состояние ячейки из
        `null` на `Dequeue`. Подождите, пока состояние ячейки не изменится.
           обновляется `Результат` (в этом случае не забудьте очистить его),
        или "комбинированный замок" становится доступным для приобретения
         */

        if (combinerLock.compareAndSet(false, true)) {
            var remove = queue.removeFirstOrNull()
            for (i in 0 until TASKS_FOR_COMBINER_SIZE) {
                var task = tasksForCombiner.get(i)
                if (task != null) {
                    if (task is Dequeue) {
                        tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                    } else if (task is Result<*>) {
                        continue
                    } else { // task as E
                        queue.addLast(task as E)
                        tasksForCombiner.set(i, Result<E?>(task))
                    }
                }
            }
            combinerLock.set(false)
            return remove
        } else {
            var randomCell = randomCellIndex()
            while (!tasksForCombiner.compareAndSet(randomCell, null, Dequeue)) {
                randomCell = randomCellIndex()
            }
            while (true) {
                if (tasksForCombiner.get(randomCell) is Result<*>) {
                    return (tasksForCombiner.getAndSet(randomCell, null) as Result<*>).value as E?
                } else if (combinerLock.compareAndSet(false, true)) {
                    val res1 = tasksForCombiner.get(randomCell)
                    tasksForCombiner.compareAndSet(randomCell, res1, null)

                    if (res1 is Result<*>) {
                        combinerLock.set(false)
                        return res1.value as E
                    }

                    var remove = queue.removeFirstOrNull()
                    for (i in 0 until TASKS_FOR_COMBINER_SIZE) {
                        var task = tasksForCombiner.get(i)
                        if (task != null) {
                            if (task is Dequeue) {
                                tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                            } else if (task is Result<*>) {
                                continue
                            } else { // task as E
                                queue.addLast(task as E)
                                tasksForCombiner.set(i, Result<E?>(task))
                            }
                        }
                    }
                    combinerLock.set(false)
                    return remove as E
                }
            }
        }
    }

    private fun randomCellIndex(): Int = ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
/*
Поместите этот токен в "задачи для объединителя" для удаления из очереди().
в очередь()-s следует поместить вставляющий элемент.
 */
private object Dequeue

// TODO: Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
// Поместите результат, завернутый в "Result", когда будет обработана операция в "задачах для объединителя".
private class Result<V>(
    val value: V
)