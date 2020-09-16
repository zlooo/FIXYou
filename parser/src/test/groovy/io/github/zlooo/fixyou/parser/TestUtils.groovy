package io.github.zlooo.fixyou.parser

class TestUtils {

    static int sumBytes(byte[] bytes) {
        int result = 0
        bytes.eachByte { result += it }
        result
    }

    static byte[] setBytes(byte[] bytesToSet, byte[] destination) {
        for (int i = 0; i < bytesToSet.length; i++) {
            destination[i] = bytesToSet[i]
        }
        return destination
    }
}
