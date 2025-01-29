import kotlinx.atomicfu.AtomicRef

/**
 * Int-to-Int hash map with open addressing and linear probes.
 */
class IntIntHashMap {
    private var core = Core(INITIAL_CAPACITY) //заменить на atomic

    /**
     * Returns value for the corresponding key or zero if this key is not present.
     * Возвращает значение для соответствующего ключа или ноль, если этот ключ отсутствует.
     * @param key a positive key.
     * @return value for the corresponding or zero if this key is not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    operator fun get(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        //если rehash,ждем пока не обновится core.next=null
        return toValue(core.getInternal(key))
    }

    /**
     * Changes value for the corresponding key and returns old value or zero if key was not present.
     * Изменяет значение для соответствующего ключа и возвращает старое значение или ноль, если ключ отсутствовал.
     * @param key   a positive key.
     * @param value a positive value.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key or value are not positive, or value is equal to
     * [Integer.MAX_VALUE] which is reserved.
     */
    fun put(key: Int, value: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        require(isValue(value)) { "Invalid value: $value" }
        return toValue(putAndRehashWhileNeeded(key, value))
    }

    /**
     * Removes value for the corresponding key and returns old value or zero if key was not present.
     * Удаляет значение для соответствующего ключа и возвращает старое значение или ноль, если ключ отсутствовал.
     * @param key a positive key.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    fun remove(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        return toValue(putAndRehashWhileNeeded(key, DEL_VALUE))
    }

    private fun putAndRehashWhileNeeded(key: Int, value: Int): Int {
        while (true) {
            val oldValue = core.putInternal(key, value)
            if (oldValue != NEEDS_REHASH) return oldValue
            core = core.rehash() //cas, возможно просто крутися пока core не обновится
            //core пустой,значит дальше пытаемся добавить кто уже забрал
            //if core пустой, то крутимся пока core.next!=null
        }
    }

    private class Core(capacity: Int) {
        // Pairs of <key, value> here, the actual
        // size of the map is twice as big.
        // Пары <ключ, значение> здесь фактический
        // размер карты в два раза больше.
        val map: IntArray = IntArray(2 * capacity) //atomic array AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)
        val shift: Int
        //val next: AtomicRef<Core>

        init {
            val mask = capacity - 1
            assert(mask > 0 && mask and capacity == 0) { "Capacity must be power of 2: $capacity" }
            shift = 32 - Integer.bitCount(mask)
        }
        //проверка связанная из-за рехеша
        fun getInternal(key: Int): Int {
            var index = index(key)
            var probes = 0
            while (map[index] != key) { // optimize for successful lookup оптимизация для успешного поиска
                //if move или result return rehash
                if (map[index] == NULL_KEY) return NULL_VALUE // not found -- no value не найдено - нет значения
                if (++probes >= MAX_PROBES) return NULL_VALUE
                if (index == 0) index = map.size
                index -= 2
            }
            // found key -- return value найденный ключ - возвращаемое значение
            // если ключ равен обертке, То возращаем внутрнее значение
            // подумать что делать если элемент MOVE
            return map[index + 1]
        }

        fun putInternal(key: Int, value: Int): Int {
            var index = index(key)
            var probes = 0
            while (map[index] != key) { // optimize for successful lookup  оптимизировать для успешного поиска
                if (map[index] == NULL_KEY) {
                    // not found -- claim this slot не найден - воспользуйтесь этим слотом
                    if (value == DEL_VALUE) return NULL_VALUE // remove of missing item, no need to claim slot
                    map[index] = key // заменить cas(пустой,key) , если получилось break

                    break
                }
                if (++probes >= MAX_PROBES) return NEEDS_REHASH
                if (index == 0) index = map.size
                index -= 2
            }
            // found key -- update value найденный ключ - обновить значение
            // если move или result вернуть rehash?
            val oldValue = map[index + 1]
            map[index + 1] = value // cas (old,value) что делать если не удался cas?while ()так как ключ уже добавили
            return oldValue
        }

        fun rehash(): Core {
            val newCore = Core(map.size) // map.length is twice the current capacity длина карты в два раза превышает текущую пропускную способность
            // next.cas () если все ок дальше, иначе получаем новый и помогаем //либо возращаем пустой core
            // вернуть пустой cor
            var index = 0
            while (index < map.size) {
                /*
                getInternal
                if (init_state==map[index]){
                    map[index+1]=move
                    index += 2
                    continue
                }
                 */

                /*
               if (map[index + 1]==null){
                   map[index+1]=move
                   index += 2
                    continue
               }
                */

                if (isValue(map[index + 1])) {
                    //получаем значение
                    // в старой хеш фиксируем значение result
                    //
                    //фиксируем значение в map
                    val result = newCore.putInternal(map[index], map[index + 1])//фиксированное значение
                    // map[index+1]=move значени
                    assert(result == 0) { "Unexpected result during rehash: $result" }
                }
                index += 2
            }
            return newCore
        }

        /**
         * Returns an initial index in map to look for a given key.
         * Возвращает начальный индекс в map для поиска заданного ключа.
         */
        fun index(key: Int): Int = (key * MAGIC ushr shift) * 2
    }
}

private const val MAGIC = -0x61c88647 // golden ratio частная константа золотого сечения
private const val INITIAL_CAPACITY = 2 // !!! DO NOT CHANGE INITIAL CAPACITY !!!
private const val MAX_PROBES = 8 // max number of probes to find an item  максимальное количество запросов для поиска элемента
private const val NULL_KEY = 0 // missing key (initial value)  отсутствует ключ (начальное значение
private const val NULL_VALUE = 0 // missing value (initial value)  отсутствует значение (начальное
private const val DEL_VALUE = Int.MAX_VALUE // mark for removed value  пометка для удаленного значения
private const val NEEDS_REHASH = -1 // returned by `putInternal` to indicate that rehash is needed возвращено `putInternal`, чтобы указать, что требуется повторная обработка
// поле -2?
private class Result<V>(
    val value: V
)

// Checks is the value is in the range of allowed values Проверяет, находится ли значение в диапазоне допустимых значений
private fun isValue(value: Int): Boolean = value in (1 until DEL_VALUE)

// Converts internal value to the public results of the methods Преобразует внутреннюю ценность в общедоступные результаты методов
private fun toValue(value: Int): Int = if (isValue(value)) value else 0