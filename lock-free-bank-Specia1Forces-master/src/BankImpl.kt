import kotlinx.atomicfu.*

/**
 * Bank implementation.
 * This class is thread-safe and lock-free using operation objects.
 *
 * This implementation is based on "A Practical Multi-Word Compare-and-Swap Operation" by T. L. Harris et al.
 * It uses a simplified and faster version of DCSS operation that relies for its correctness on the fact that
 * Account instances in [accounts] array never suffer from ABA problem.
 * See also "Practical lock-freedom" by Keir Fraser. See [acquire] method.
 *
 * Эта реализация основана на "Практической операции сравнения и замены нескольких слов" Т. Л. Харриса и др.
 * В ней используется упрощенная и быстрая версия операции DCSS, корректность которой основана на том факте, что
 * Экземпляры учетных записей в массиве [accounts] никогда не страдают от проблемы ABA.
 * Смотрите также "Практическая защита от блокировок" Кира Фрейзера. Смотрите метод [приобретения].
 *
 * :TODO: This implementation has to be completed, so that it is thread-safe and lock-free.
 */
class BankImpl(override val numberOfAccounts: Int) : Bank {
    /**
     * An array of accounts.
     * Account instances here are never reused (there is no ABA).
     * Массив учетных записей.
     * Экземпляры учетных записей здесь никогда не используются повторно (ABA отсутствует).
     */
    private val accounts = atomicArrayOfNulls<Account>(numberOfAccounts)

    init {
        for (i in 0 until numberOfAccounts) accounts[i].value = Account(0)
    }

    private fun account(index: Int) = accounts[index].value!!

    override fun getAmount(index: Int): Long {
        while (true) {
            val account = account(index)
            /*
             * If there is a pending operation on this account, then help to complete it first using
             * its invokeOperation method. If the result is false then there is no pending operation,
             * thus the account amount can be safely returned.
             *
             * Если на этом счете есть незавершенная операция, то сначала помогите выполнить ее, используя метод invokeOperation.
             * Если результат равен false, то незавершенной операции нет, поэтому сумма на счете может быть возвращена без проблем.
             */
            if (!account.invokeOperation()) return account.amount
        }
    }

    override val totalAmount: Long
        get() {
            /**
             * This operation requires atomic read of all accounts, thus it creates an operation descriptor.
             * Operation's invokeOperation method acquires all accounts, computes the total amount, and releases
             * all accounts. This method returns the result.
             * Эта операция требует атомарного считывания всех учетных записей, таким образом, создается дескриптор операции.
             * Метод invokeOperation операции получает все учетные записи, вычисляет общую сумму и освобождает
             * все учетные записи. Этот метод возвращает результат.
             */
            val op = TotalAmountOp()
            op.invokeOperation()
            return op.sum
        }

    override fun deposit(index: Int, amount: Long): Long { // First, validate method per-conditions
        require(amount > 0) { "Invalid amount: $amount" }
        check(amount <= MAX_AMOUNT) { "Overflow" }
        /*
         * This operation depends only on a single account, thus it can be directly
         * performed using a regular lock-free compareAndSet loop.
         * Эта операция зависит только от одной учетной записи, поэтому ее можно
            выполнить напрямую, используя обычный цикл compareAndSet без блокировок.
         */
        while (true) {
            val account = account(index)
            /*
             * If there is a pending operation on this account, then help to complete it first using
             * its invokeOperation method. If the result is false then there is no pending operation,
             * thus the account can be safely updated.
             * Если для этой учетной записи есть ожидающая операция, то сначала помогите выполнить ее, используя
             метод invokeOperation. Если результат равен false, то ожидающей операции нет,
            поэтому учетную запись можно безопасно обновить.
             */
            if (account.invokeOperation()) continue
            check(account.amount + amount <= MAX_AMOUNT) { "Overflow" }
            val updated = Account(account.amount + amount)
            if (accounts[index].compareAndSet(account, updated)) return updated.amount
        }
    }

    override fun withdraw(index: Int, amount: Long): Long {
        // todo: write withdraw operation using deposit as an example
        //запишите операцию вывода средств с использованием депозита в качестве примера
        /*
         * Basically, implementation of this method must perform the logic of the following code "atomically":
         * По сути, реализация этого метода должна выполнять логику следующего кода "атомарно".:
         */

        require(amount > 0) { "Invalid amount: $amount" }
        while (true) {
            val account = account(index)
            if (account.invokeOperation()) continue
            check(account.amount - amount >= 0) { "Underflow" }
            val updated = Account(account.amount - amount)

            if (accounts[index].compareAndSet(account, updated)) return updated.amount
        }

    }

    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        // First, validate method per-conditions
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        check(amount <= MAX_AMOUNT) { "Underflow/overflow" }
        /**
         * This operation requires atomic read of two accounts, thus it creates an operation descriptor.
         * Operation's invokeOperation method acquires both accounts, computes the result of operation
         * (if a form of error message), and releases both accounts. This method throws the exception with
         * the corresponding message if needed.
         * Эта операция требует атомарного считывания двух учетных записей, таким образом, создается дескриптор операции.
         *  Метод invokeOperation, используемый в Operation, получает данные обеих учетных записей, вычисляет результат операции
         * (если это сообщение об ошибке) и освобождает обе учетные записи. При необходимости этот метод генерирует исключение с
         * соответствующим сообщением.
         */
        val op = TransferOp(fromIndex, toIndex, amount)
        /*
        try {
            op.invokeOperation()
        } catch (e: Exception) {
            println(op.errorMessage?.let { error(it) })
        }
         */
        op.invokeOperation()
        op.errorMessage?.let { error(it) }
    }

    /**
     * This is an implementation of a restricted form of Harris DCSS operation:
     * It atomically checks that op.completed is false and replaces accounts[index] with AcquiredAccount instance
     * that hold a reference to the op.
     * This method returns null if op.completed is true.
     * Это реализация ограниченной формы операции Harris DCSS:
     *  Она атомарно проверяет, что значение op.completed равно false, и заменяет учетные записи[index] на приобретенный экземпляр учетной
     * записи, который содержит ссылку на op.
     * Этот метод возвращает значение null, если значение op.completed равно true.
     */
    private fun acquire(index: Int, op: Op): AcquiredAccount? {
        // todo: write the implementation of this method with the following logic:
        /*
         * This method must loop trying to replace accounts[index] with an instance of
         *     new AcquiredAccount(<old-amount>, op) until that successfully happens and return the
         *     instance of AcquiredAccount in this case.
         *
         * If current account is already "Acquired" by another operation, then this method must help that
         * other operation by invoking "invokeOperation" and continue trying.
         *
         * Because accounts[index] does not have an ABA problem, there is no need to implement full-blown
         * DCSS operation with descriptors for DCSS operation as explained in Harris CASN work. A simple
         * lock-free compareAndSet loop suffices here if op.completed is checked after the accounts[index]
         * is read.
         *
         * Basically, implementation of this method must perform the logic of the following code "atomically":
         * Этот метод должен повторять попытку замены accounts[index] экземпляром
           новой приобретенной учетной записи(<old-amount>, op) до тех пор, пока это не произойдет успешно, и
           в этом случае возвращает экземпляр приобретенной учетной записи.
           Если текущая учетная запись уже "приобретена" другой операцией, то этот метод должен помочь этой
           другой операции, вызвав "invoke Operation" и продолжив попытки.
           Поскольку accounts[index] не имеет проблемы с ABA, нет необходимости в полномасштабной реализации
           МОЖЕТ работать функция DCSS с дескрипторами для работы DCS, как описано в Harris. Простая
           здесь достаточно цикла compareAndSet без блокировок, если op.completed проверяется после чтения accounts[index].
           По сути, реализация этого метода должна выполнять логику следующего кода "атомарно".:
         */

        //походу просто цикл while + op.inovoke
        /*
        if (op.completed) return null
        val account = account(index)
        val acquiredAccount = AcquiredAccount(account.amount, op)
        accounts[index].value = acquiredAccount
        return acquiredAccount
         */
        while (true) {
            val account = account(index)
            if (op.completed) return null
            if (account is AcquiredAccount) {
                if (account.op === op) {
                    return account
                }
                if (account.invokeOperation()) continue
            }
            val acquiredAccount = AcquiredAccount(account.amount, op)
            if (accounts[index].compareAndSet(account, acquiredAccount)) {
                return acquiredAccount
            }
        }
    }

    /**
     * Releases an account that was previously acquired by [acquire].
     * This method does nothing if the account at index is not currently acquired.
     * Освобождает учетную запись, которая была ранее приобретена с помощью [приобрести].
     * Этот метод ничего не делает, если учетная запись в index в настоящее время не приобретена.
     */
    private fun release(index: Int, op: Op) {
        assert(op.completed) // must be called only on operations that were already completed должен вызываться только для тех операций, которые уже были выполнены
        val account = account(index)
        if (account is AcquiredAccount && account.op === op) {
            // release performs update at most once while the account is still acquired
            val updated = Account(account.newAmount)
            accounts[index].compareAndSet(account, updated)
        }
    }

    /**
     * Immutable account data structure.
     * @param amount Amount of funds in this account.
     * Неизменяемая структура данных учетной записи.
     * @param amount - Сумма средств на этом счете.
     */
    private open class Account(val amount: Long) {
        /**
         * Invokes operation that is pending on this account.
         * This implementation returns false (no pending operation), other implementations return true.
         * Вызывает операцию, ожидающую выполнения в этой учетной записи.
         *  Эта реализация возвращает значение false (нет ожидающей операции), другие реализации возвращают значение true.
         */
        open fun invokeOperation(): Boolean = false
    }

    /**
     * Account that was acquired as a part of in-progress operation that spans multiple accounts.
     * учетная запись, которая была приобретена в рамках текущей операции, охватывающей несколько учетных записей.
     * @see acquire
     */
    private class AcquiredAccount(
        var newAmount: Long, // New amount of funds in this account when op completes. Новая сумма средств на этом счете после завершения операции.
        val op: Op
    ) : Account(newAmount) {
        override fun invokeOperation(): Boolean {
            op.invokeOperation()
            return true
        }
    }

    /**
     * Abstract operation that acts on multiple accounts.
     * Абстрактная операция, которая выполняется с несколькими учетными записями.
     */
    private abstract inner class Op {
        /**
         * True when operation has completed.
         * Значение True после завершения операции.
         */
        @Volatile
        var completed = false

        abstract fun invokeOperation()
    }

    /**
     * Descriptor for [totalAmount] operation.
     * Дескриптор для операции [общая сумма].
     */
    private inner class TotalAmountOp : Op() {
        /**
         * The result of getTotalAmount operation is stored here before setting
         * [completed] to true.
         * Результат операции getTotalAmount сохраняется здесь перед установкой значения [завершено] в true.
         */
        var sum = 0L

        override fun invokeOperation() {
            var sum = 0L
            var acquired = 0
            while (acquired < numberOfAccounts) {
                val account = acquire(acquired, this) ?: break
                sum += account.newAmount
                acquired++
            }
            if (acquired == numberOfAccounts) {
                /*
                 * If i == n, then all acquired accounts were not null and full sum was calculated.
                 * this.sum = sum assignment below has a benign data race. Multiple threads might to this assignment
                 * concurrently, however, they are all guaranteed to be assigning the same value.
                 */
                /*
                 * Если i == n, то все полученные учетные записи не были нулевыми, и была рассчитана полная сумма.
                 * приведенное ниже назначение.sum = sum приводит к неопасной гонке данных. Это назначение может выполняться несколькими потоками
                 * однако, все они гарантированно присваивают одно и то же значение одновременно.
                 */
                this.sum = sum
                completed = true // volatile write to completed field _after_ the sum was written
            }
            /*
             * To ensure lock-freedom, we must release all accounts even if this particular helper operation
             * had failed to acquire all of them before somebody else had completed the operations.
             * By releasing all accounts for completed operation we ensure progress of other operations.
             */
            /*
             * Чтобы гарантировать отсутствие блокировки, мы должны разблокировать все учетные записи, даже если для этой конкретной вспомогательной операции
             * не удалось получить доступ ко всем из них до того, как кто-то другой завершил операции.
             * Разблокируя все учетные записи для завершенной операции, мы гарантируем выполнение других операций.
             */
            for (i in 0 until numberOfAccounts) {
                release(i, this)
            }
        }
    }

    /**
     * Descriptor for [transfer] operation.
     */
    private inner class TransferOp(val fromIndex: Int, val toIndex: Int, val amount: Long) : Op() {
        var errorMessage: String? = null

        override fun invokeOperation() {
            // todo: write implementation for this method, use TotalAmountOp as an example
            /*
             * In the implementation of this operation only two accounts (with fromIndex and toIndex) needs
             * to be acquired. Unlike TotalAmountOp, this operation has its own result in errorMessage string,
             * and it must also update AcquiredAccount.newAmount fields before setting completed to true
             * and invoking release on those acquired accounts.
             *
             * Basically, implementation of this method must perform the logic of the following code "atomically":
             */

            /*
             * Для выполнения этой операции необходимо получить только две учетные записи (с fromIndex и toIndex).
             * В отличие от операции "Общая сумма", эта операция имеет свой собственный результат в строке сообщения об ошибке,
              и она также должна обновить поля "Приобретенная учетная запись.новая сумма" перед установкой значения "завершено" в значение "истина"
             * и вызовом освобождения для этих приобретенных учетных записей.
             * В принципе, реализация этого метода должна выполнять логику следующего кода "атомарно".:
             */

            // нужно использовать еще acquire это связано как-то с аккаунтами
            //val from = account(fromIndex)
            //val to = account(toIndex)
            val from: AcquiredAccount?
            val to: AcquiredAccount?
            if (fromIndex < toIndex) {
                from = acquire(fromIndex, this)
                to = acquire(toIndex, this)
            } else {
                to = acquire(toIndex, this)
                from = acquire(fromIndex, this)
            }
            if (from != null) {
                if (to != null) {
                    when {
                        amount > from.amount -> errorMessage = "Underflow"
                        to.amount + amount > MAX_AMOUNT -> errorMessage = "Overflow"
                        else -> {
                            //accounts[fromIndex].value = Account(from.amount - amount)
                            //accounts[toIndex].value = Account(to.amount + amount)
                            from.newAmount = from.amount - amount
                            to.newAmount = to.amount + amount
                        }
                    }
                }
                // обновить
                completed = true
            }

            if (fromIndex < toIndex) {
                release(fromIndex, this)
                release(toIndex, this)
            } else {
                release(toIndex, this)
                release(fromIndex, this)
            }
        }
    }
}