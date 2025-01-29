/**
 * Bank interface.
 */
interface Bank {
    /**
     * Returns number of accounts in this bank. Возвращает количество счетов в этом банке.
     */
    val numberOfAccounts: Int

    /**
     * Returns current amount in the specified account. Возвращает текущую сумму на указанном счете.
     *
     * @param index account index from 0 to [n][numberOfAccounts]-1.
     * @return amount in account.
     * @throws IndexOutOfBoundsException when index is invalid account index.
     */
    fun getAmount(index: Int): Long

    /**
     * Returns total amount deposited in this bank. Возвращает общую сумму, внесенную в этот банк.
     */
    val totalAmount: Long

    /**
     * Deposits specified amount to account. Внесите указанную сумму на счет.
     *
     * @param index account index from 0 to [n][numberOfAccounts]-1.
     * @param amount positive amount to deposit.
     * @return resulting amount in account.
     * @throws IllegalArgumentException when amount <= 0.
     * @throws IndexOutOfBoundsException when index is invalid account index.
     * @throws IllegalStateException when deposit will overflow account above [MAX_AMOUNT].
     */
    fun deposit(index: Int, amount: Long): Long

    /**
     * Withdraws specified amount from account. Снимает указанную сумму со счета.
     *
     * @param index account index from 0 to [n][numberOfAccounts]-1.
     * @param amount positive amount to withdraw.
     * @return resulting amount in account.
     * @throws IllegalArgumentException when amount <= 0.
     * @throws IndexOutOfBoundsException when index is invalid account index.
     * @throws IllegalStateException when account does not enough to withdraw.
     */
    fun withdraw(index: Int, amount: Long): Long

    /**
     * Transfers specified amount from one account to another account. Переводит указанную сумму с одного счета на другой.
     *
     * @param fromIndex account index to withdraw from.
     * @param toIndex account index to deposit to.
     * @param amount positive amount to transfer.
     * @throws IllegalArgumentException when amount <= 0 or fromIndex == toIndex.
     * @throws IndexOutOfBoundsException when account indices are invalid.
     * @throws IllegalStateException when there is not enough funds in source account or too much in target one.
     */
    fun transfer(fromIndex: Int, toIndex: Int, amount: Long)

    companion object {
        /**
         * The maximal amount that can be kept in a bank account. Максимальная сумма, которую можно хранить на банковском счете.
         */
        const val MAX_AMOUNT = 1000000000000000L
    }
}