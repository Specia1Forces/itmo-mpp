/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author :Stolbov Dmitry
 */
class Solution : MonotonicClock {
    private var copy1_1 by RegularInt(0)
    private var copy1_2 by RegularInt(0)
    private var copy1_3 by RegularInt(0)
    private var copy2_1 by RegularInt(0)
    private var copy2_2 by RegularInt(0)
    private var copy2_3 by RegularInt(0)

    override fun write(time: Time) {
        // с2 слева направо
        copy2_1 = time.d1
        copy2_2 = time.d2
        copy2_3 = time.d3

        // c1 справо налево

        copy1_3 = copy2_3
        copy1_2 = copy2_2
        copy1_1 = copy2_1

    }

    override fun read(): Time {
        // r1 слева направо
        val reader1_1 = copy1_1
        val reader1_2 = copy1_2
        val reader1_3 = copy1_3
        // r2 справо налево
        val reader2_3 = copy2_3
        val reader2_2 = copy2_2
        val reader2_1 = copy2_1
        //p := длина максимального общего префикса r1 и r2
        //Вернуть любое значение между [r1[1], r1[2], ..., r1[p+1], 9, 9, 9, ...] и [r2[1], r2[2], ..., r2[p+1], 0, 0, 0, ...]
        if (reader1_1 == reader2_1 && reader1_2 == reader2_2 && reader1_3 == reader2_3) {
            return Time(reader1_1, reader1_2, reader1_3)
        }

        if (reader1_1 != reader2_1) {
            return Time(reader2_1, 0, 0)
        } else if (reader1_2 != reader2_2) {
            return Time(reader2_1, reader2_2, 0)
        } else {
            return Time(reader2_1, reader2_2, reader2_3)
        }
    }
}