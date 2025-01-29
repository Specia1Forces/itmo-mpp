interface Stack<E> {
    /**
     * Adds the specified [element] to the top of this stack. Добавляет указанный [элемент] наверх этого стека.
     */
    fun push(element: E)

    /**
     * Retrieves the top element from this stack and returns it; Извлекает верхний элемент из этого стека и возвращает его;
     * returns `null` if the stack is empty. * возвращает `null`, если стек пуст.
     */
    fun pop(): E?
}