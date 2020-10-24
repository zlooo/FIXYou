package io.github.zlooo.fixyou.parser

class TestUtils {

    static int sumBytes(byte[] bytes) {
        int result = 0
        bytes.eachByte { result += it }
        result
    }
}
