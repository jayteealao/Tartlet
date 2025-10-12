package io.yumemi.something

fun generateFibi() = sequence {
    var a = 1
    yield(a)
    var b = 2
    yield(b)
    while (true) {
        val c = a + b
        yield(c)
        a = b
        b = c
    }
}
