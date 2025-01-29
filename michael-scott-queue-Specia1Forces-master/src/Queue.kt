interface Queue<E> {

    /**
     * Adds the specified [element] to the queue. Добавляет указанный [элемент] в очередь.
     */
    fun enqueue(element: E)

    /**
     * Retrieves the first element from the queue and returns it; Извлекает первый элемент из очереди и возвращает его;
     * returns `null` if the queue is empty. возвращает значение "null", если очередь пуста.
     */
    fun dequeue(): E?

    /**
     * Validates the data structure state at the end of execution. Проверяет состояние структуры данных в конце выполнения.
     * FOR TEST PURPOSE ONLY. ТОЛЬКО ДЛЯ ЦЕЛЕЙ ТЕСТИРОВАНИЯ.
     */
    fun validate() {}
}