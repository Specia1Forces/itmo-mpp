import java.util.concurrent.locks.ReentrantLock

/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe. Эта реализация должна быть потокобезопасной.
 *
 * @author :TODO: Stolbov Dmitry
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }



    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * :TODO: This method has to be made thread-safe.
     * Этот метод должен быть потокобезопасным.
     */
    override fun getAmount(index: Int): Long {

        val account = accounts[index]
        account.lock()
        try {
            return accounts[index].amount    // do something
        } finally {
            account.unlock()
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     * Этот метод должен быть потокобезопасным.
     */
    override val totalAmount: Long
        get() { //deadlock?
            for (i in 0..<accounts.size) {
                accounts[i].lock()
            }
            try {
                return accounts.sumOf { account ->
                    account.amount
                }   // do something
            } finally {
                for (i in 0..<accounts.size) { //i in (accounts.size-1) downTo 1
                    accounts[i].unlock()
                }
            }

        }

    /**
     * :TODO: This method has to be made thread-safe.
     *
     * Этот метод должен быть потокобезопасным.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }

        val account = accounts[index]
        account.lock()
        try {

            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            return account.amount  // do something
        } finally {
            account.unlock()
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     *
     * Этот метод должен быть потокобезопасным.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock()
        try {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        } finally {
            account.unlock()
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     *
     * Этот метод должен быть потокобезопасным.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }

        var account1 = accounts[fromIndex]
        var account2 = accounts[toIndex]

        if (fromIndex < toIndex) {
            account1.lock()
            account2.lock()
            try {
                val from = accounts[fromIndex]
                val to = accounts[toIndex]
                check(amount <= from.amount) { "Underflow" }
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            } finally {
                account1.unlock()
                account2.unlock()
            }
        }else{
            account2.lock()
            account1.lock()
            try {
                val from = accounts[fromIndex]
                val to = accounts[toIndex]
                check(amount <= from.amount) { "Underflow" }
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            } finally {
                account2.unlock()
                account1.unlock()
            }
        }

    }

    /**
     * Private account data structure. Структура данных личной учетной записи.
     */
    class Account {
        /**
         * Amount of funds in this account.
         * Сумма средств на этом счете.
         */
        var amount: Long = 0

        var lock = ReentrantLock()

        fun lock(){
            lock.lock()
        }

        fun unlock(){
            lock.unlock()
        }
    }
}