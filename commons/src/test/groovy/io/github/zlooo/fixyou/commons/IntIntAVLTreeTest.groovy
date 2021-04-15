package io.github.zlooo.fixyou.commons

import spock.lang.Specification

import java.util.concurrent.ThreadLocalRandom

class IntIntAVLTreeTest extends Specification {

    private IntIntAVLTree avlTree = new IntIntAVLTree(10)

    void setup() {
        avlTree.put(1, 1)
        avlTree.put(2, 2)
        avlTree.put(3, 3)
        avlTree.put(4, 4)
        avlTree.put(5, 5)
    }

    def "should get data"() {
        expect:
        avlTree.get(key) == value

        where:
        key | value
        1   | 1
        2   | 2
        4   | 4
        3   | 3
        5   | 5
        6   | IntIntAVLTree.INVALID_IDX
    }

    def "should remove value"() {
        when:
        avlTree.remove(3)

        then:
        avlTree.get(3) == IntIntAVLTree.INVALID_IDX
        check(avlTree)
    }

    def "should get floor entry"() {
        setup:
        avlTree.remove(1)
        avlTree.put(10, 10)
        avlTree.put(20, 20)
        avlTree.put(30, 30)

        expect:
        avlTree.floorValue(key) == value

        where:
        key | value
        1   | IntIntAVLTree.INVALID_IDX
        2   | 2
        3   | 3
        4   | 4
        5   | 5
        6   | 5
        10  | 10
        11  | 10
        40  | 30
    }

    //warning long running test, it hasn't hanged it just takes a while to run
    def "10k put test"() {
        setup:
        avlTree = new IntIntAVLTree(10000)
        final ThreadLocalRandom random = ThreadLocalRandom.current()
        int[] keysToSearch = new int[10000]

        when:
        for (int i = 0; i < 10000; i++) {
            //I know that this could potentially lead to flaky test, I just wanted to have some degree of confidence that at least most of possible cases are tested and I can't exactly specify all of them
            int key = random.nextInt(Integer.MAX_VALUE);
            int value = random.nextInt(Integer.MAX_VALUE);
            avlTree.put(key, value);
            assert check(avlTree)
            keysToSearch[i] = key
        }

        then:
        for (int i = 0; i < 10000; i++) {
            assert avlTree.get(keysToSearch[i]) != IntIntAVLTree.INVALID_IDX
        }
        check(avlTree)
    }

    //warning long running test, it hasn't hanged it just takes a while to run
    def "10k remove test"() {
        setup:
        avlTree = new IntIntAVLTree(10000)
        final ThreadLocalRandom random = ThreadLocalRandom.current()
        int[] keys = new int[10000]
        int[] values = new int[10000]
        for (int i = 0; i < 10000; i++) {
            //I know that this could potentially lead to flaky test, I just wanted to have some degree of confidence that at least most of possible cases are tested and I can't exactly specify all of them
            int key = random.nextInt(Integer.MAX_VALUE)
            int value = random.nextInt(Integer.MAX_VALUE)
            avlTree.put(key, value)
            keys[i] = key
            values[i] = value
        }

        when:
        for (int i = 0; i < keys.length; i++) {
            def removed = avlTree.remove(keys[i])
            assert check(avlTree)
            assert removed == values[i]
        }

        then:
        avlTree.size() == 0
        for (int key : keys) {
            assert avlTree.get(key) == IntIntAVLTree.INVALID_IDX
        }
        check(avlTree)
    }

    private static boolean check(IntIntAVLTree avlTree) {
        // check root
        if (avlTree.size() == 0 && (avlTree.@rootIndex != IntIntAVLTree.INVALID_IDX)) {
            // invalid root
            return false
        }
        if (avlTree.size() && avlTree.@rootIndex >= avlTree.size()) {
            // root out of bounds
            return false
        }

        // check tree
        for (int i = 0; i < avlTree.size(); ++i) {
            if ((avlTree.@children[i].left != IntIntAVLTree.INVALID_IDX) && (!(avlTree.@keys[avlTree.@children[i].left] < avlTree.@keys[i]) || (avlTree.@keys[avlTree.@children[i].left] == avlTree.@keys[i]))) {
                // wrong key order to the left
                return false
            }
            if ((avlTree.@children[i].right != IntIntAVLTree.INVALID_IDX) && ((avlTree.@keys[avlTree.@children[i].right] < avlTree.@keys[i]) || (avlTree.@keys[avlTree.@children[i].right] == avlTree.@keys[i]))) {
                // wrong key order to the right
                return false
            }
            final int parent = avlTree.@parentIndexes[i]
            if ((i != avlTree.@rootIndex) && (parent == IntIntAVLTree.INVALID_IDX)) {
                // no parent
                return false
            }
            if ((i == avlTree.@rootIndex) && (parent != IntIntAVLTree.INVALID_IDX)) {
                // invalid root parent
                return false
            }
        }
        // check passed
        return true
    }
}
