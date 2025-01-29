/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe.
 *
 * @author :TODO: Stolbov Dmitry
 */
import java.util.concurrent.locks.ReentrantReadWriteLock

class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun getAmount(index: Int): Long {
        val account = accounts[index]
        account.readLock()
        try {
            return accounts[index].amount
        } finally {
            account.readUnlock()
        }

    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override val totalAmount: Long
        get() {
            for (i in 0..<accounts.size) {
                accounts[i].readLock()
            }
            try {
                return accounts.sumOf { account ->
                    account.amount
                }
            } finally {
                for (i in 0..<accounts.size) { //i in (accounts.size-1) downTo 1
                    accounts[i].readUnlock()
                }
            }
        }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }

        val account = accounts[index]
        account.writeLock()
        try {

            check(amount <= Bank.MAX_AMOUNT && account.amount + amount <= Bank.MAX_AMOUNT) { "Overflow" }
            account.amount += amount
            return account.amount
        } finally {
            account.writeUnlock()
        }

    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }

        val account = accounts[index]
        account.writeLock()
        try {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        } finally {
            account.writeUnlock()
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }

        var account1 = accounts[fromIndex]
        var account2 = accounts[toIndex]

        if (fromIndex < toIndex) {
            account1.writeLock()
            account2.writeLock()
            try {
                val from = accounts[fromIndex]
                val to = accounts[toIndex]
                check(amount <= from.amount) { "Underflow" }
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            } finally {
                account1.writeUnlock()
                account2.writeUnlock()
            }
        } else {
            account2.writeLock()
            account1.writeLock()
            try {
                val from = accounts[fromIndex]
                val to = accounts[toIndex]
                check(amount <= from.amount) { "Underflow" }
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            } finally {
                account2.writeUnlock()
                account1.writeUnlock()
            }
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun consolidate(fromIndices: List<Int>, toIndex: Int) {
        require(fromIndices.isNotEmpty()) { "empty fromIndices" }
        require(fromIndices.distinct() == fromIndices) { "duplicates in fromIndices" }
        require(toIndex !in fromIndices) { "toIndex in fromIndices" }

        val fromIndicesSorted = fromIndices.sorted()
        val fromList = fromIndicesSorted.map { Pair(it, accounts[it]) }
        val to = accounts[toIndex]
        var flagToIndex = false

        // Сначала блокируем необходимые аккаунты
        for ((index, account) in fromList) {
            if (!flagToIndex) {
                if (index < toIndex) {
                    account.writeLock()
                } else {
                    to.writeLock()
                    account.writeLock()
                    flagToIndex = true
                }
            } else {
                account.writeLock()
            }
        }

        // Если ни один аккаунт не был выше toIndex, блокируем to
        if (!flagToIndex) {
            to.writeLock()
        }

        try {
            // Считаем сумму переводимых средств
            val amount = fromList.sumOf { it.second.amount }

            // Проверяем на переполнение
            check(to.amount + amount <= Bank.MAX_AMOUNT) { "Overflow" }

            // Обнуляем средства на исходных аккаунтах и добавляем их к целевому аккаунту
            for ((_, account) in fromList) {
                account.amount = 0
            }
            to.amount += amount
        } finally {
            // Разблокируем аккаунты
            flagToIndex = false
            for ((index, account) in fromList) {
                if (!flagToIndex) {
                    if (index < toIndex) {
                        account.writeUnlock()
                    } else {
                        to.writeUnlock()
                        account.writeUnlock()
                        flagToIndex = true
                    }
                } else {
                    account.writeUnlock()
                }
            }
            if (!flagToIndex) {
                to.writeUnlock()
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0

        var lock = ReentrantReadWriteLock()

        //var readLock = lock.readLock()

        //var writeLock = lock.writeLock()

        fun readLock() {
            lock.readLock().lock()
        }

        fun readUnlock() {
            lock.readLock().unlock()
        }

        fun writeLock() {
            lock.writeLock().lock()
        }

        fun writeUnlock() {
            lock.writeLock().unlock()
        }

    }
}