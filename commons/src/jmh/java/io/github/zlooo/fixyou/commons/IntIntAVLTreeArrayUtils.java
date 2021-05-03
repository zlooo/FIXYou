package io.github.zlooo.fixyou.commons;

import io.github.zlooo.fixyou.utils.ArrayUtils;

/**
 * This class is practically speaking a Java port of <a href="https://github.com/mpaland/avl_array">https://github.com/mpaland/avl_array</a> that's tailored to hold int - int key - value pairs
 */
public class IntIntAVLTreeArrayUtils {

    public static final int INVALID_IDX = -1;
    private static final byte ONE = 1;
    private static final byte ZERO = 0;
    private static final byte MINUS_ONE = -1;
    private static final int REBALANCE_THRESHOLD = 2;
    private static final int MINUS_REBALANCE_THRESHOLD = -2;

    private final int[] parentIndexes;
    private final int[] keys;
    private final int[] values;
    private final byte[] balances;
    private final Node[] children;
    private final int maxSize;
    private int size;
    private int rootIndex = INVALID_IDX;

    public IntIntAVLTreeArrayUtils(int size) {
        this.parentIndexes = new int[size];
        this.keys = new int[size];
        this.values = new int[size];
        this.balances = new byte[size];
        this.children = new Node[size]; //maybe child should just be int[2*size]?
        for (int i = 0; i < size; i++) {
            ArrayUtils.putElementAt(children, i, new Node());
        }
        this.maxSize = size;
    }

    public int get(int key) {
        final int node = findNode(key);
        if (node == INVALID_IDX) {
            return INVALID_IDX;
        } else {
            return ArrayUtils.getElementAt(values, node);
        }
    }

    public boolean put(int key, int val) {
        if (rootIndex == INVALID_IDX) {
            ArrayUtils.putElementAt(keys, size, key);
            ArrayUtils.putElementAt(values, size, val);
            ArrayUtils.putElementAt(balances, size, ZERO);
            ArrayUtils.getElementAt(children, size).reset();
            setParent(size, INVALID_IDX);
            rootIndex = size++;
            return true;
        }
        boolean insertResult = false;
        for (int i = rootIndex; i != INVALID_IDX; i = ((key < ArrayUtils.getElementAt(keys, i)) ? ArrayUtils.getElementAt(children, i).left : ArrayUtils.getElementAt(children, i).right)) {
            if (key < ArrayUtils.getElementAt(keys, i)) {
                if (ArrayUtils.getElementAt(children, i).left == INVALID_IDX) {
                    if (size >= maxSize) {
                        break;
                    }
                    ArrayUtils.putElementAt(keys, size, key);
                    ArrayUtils.putElementAt(values, size, val);
                    ArrayUtils.putElementAt(balances, size, ZERO);
                    ArrayUtils.getElementAt(children, size).reset();
                    setParent(size, i);
                    ArrayUtils.getElementAt(children, i).left = size++;
                    insertBalance(i, ONE);
                    insertResult = true;
                    break;
                }
            } else if (ArrayUtils.getElementAt(keys, i) == key) {
                ArrayUtils.putElementAt(values, i, val);
                insertResult = true;
                break;
            } else {
                if (ArrayUtils.getElementAt(children, i).right == INVALID_IDX) {
                    if (size >= maxSize) {
                        break;
                    }
                    ArrayUtils.putElementAt(keys, size, key);
                    ArrayUtils.putElementAt(values, size, val);
                    ArrayUtils.putElementAt(balances, size, ZERO);
                    ArrayUtils.getElementAt(children, size).reset();
                    setParent(size, i);
                    ArrayUtils.getElementAt(children, i).right = size++;
                    insertBalance(i, MINUS_ONE);
                    insertResult = true;
                    break;
                }
            }
        }
        return insertResult;
    }

    public void clear() {
        size = 0;
        rootIndex = INVALID_IDX;
    }

    public int remove(int key) {
        final int node = findNode(key);
        final int removedValue;
        if (node == INVALID_IDX) {
            return INVALID_IDX;
        } else {
            removedValue = ArrayUtils.getElementAt(values, node);
        }

        final int left = ArrayUtils.getElementAt(children, node).left;
        final int right = ArrayUtils.getElementAt(children, node).right;

        if (left == INVALID_IDX) {
            noLeftChild(node, right);
        } else if (right == INVALID_IDX) {
            noRightChild(node, left);
        } else {
            int successor = right;
            if (ArrayUtils.getElementAt(children, successor).left == INVALID_IDX) {
                noLeftSuccessor(node, left, successor);
            } else {
                while (ArrayUtils.getElementAt(children, successor).left != INVALID_IDX) {
                    successor = ArrayUtils.getElementAt(children, successor).left;
                }

                final int parent = ArrayUtils.getElementAt(parentIndexes, node);
                final int successorParent = ArrayUtils.getElementAt(parentIndexes, successor);
                final int successorRight = ArrayUtils.getElementAt(children, successor).right;

                if (ArrayUtils.getElementAt(children, successorParent).left == successor) {
                    ArrayUtils.getElementAt(children, successorParent).left = successorRight;
                } else {
                    ArrayUtils.getElementAt(children, successorParent).right = successorRight;
                }

                setParent(successorRight, successorParent);
                setParent(successor, parent);
                setParent(right, successor);
                setParent(left, successor);
                ArrayUtils.getElementAt(children, successor).left = left;
                ArrayUtils.getElementAt(children, successor).right = right;
                ArrayUtils.putElementAt(balances, successor, ArrayUtils.getElementAt(balances, node));

                if (node == rootIndex) {
                    rootIndex = successor;
                } else {
                    if (ArrayUtils.getElementAt(children, parent).left == node) {
                        ArrayUtils.getElementAt(children, parent).left = successor;
                    } else {
                        ArrayUtils.getElementAt(children, parent).right = successor;
                    }
                }
                deleteBalance(successorParent, MINUS_ONE);
            }
        }
        size--;

        // relocate the node at the end to the deleted node, if it's not the deleted one
        if (node != size) {
            moveData(size, node);
        }

        return removedValue;
    }

    private void moveData(int nodeFrom, int nodeTo) {
        int parent = INVALID_IDX;
        if (rootIndex == nodeFrom) {
            rootIndex = nodeTo;
        } else {
            parent = ArrayUtils.getElementAt(parentIndexes, nodeFrom);
            if (ArrayUtils.getElementAt(children, parent).left == nodeFrom) {
                ArrayUtils.getElementAt(children, parent).left = nodeTo;
            } else {
                ArrayUtils.getElementAt(children, parent).right = nodeTo;
            }
        }

        // correct childs parent
        setParent(ArrayUtils.getElementAt(children, nodeFrom).left, nodeTo);
        setParent(ArrayUtils.getElementAt(children, nodeFrom).right, nodeTo);

        // move content
        ArrayUtils.putElementAt(keys, nodeTo, ArrayUtils.getElementAt(keys, nodeFrom));
        ArrayUtils.putElementAt(values, nodeTo, ArrayUtils.getElementAt(values, nodeFrom));
        ArrayUtils.putElementAt(balances, nodeTo, ArrayUtils.getElementAt(balances, nodeFrom));
        ArrayUtils.getElementAt(children, nodeTo).copyFrom(ArrayUtils.getElementAt(children, nodeFrom));
        setParent(nodeTo, parent);
    }

    private void noLeftSuccessor(int node, int left, int successor) {
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);
        ArrayUtils.getElementAt(children, successor).left = left;
        ArrayUtils.putElementAt(balances, successor, ArrayUtils.getElementAt(balances, node));
        setParent(successor, parent);
        setParent(left, successor);

        if (node == rootIndex) {
            rootIndex = successor;
        } else {
            if (ArrayUtils.getElementAt(children, parent).left == node) {
                ArrayUtils.getElementAt(children, parent).left = successor;
            } else {
                ArrayUtils.getElementAt(children, parent).right = successor;
            }
        }
        deleteBalance(successor, ONE);
    }

    private void noRightChild(int node, int left) {
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);
        if (parent != INVALID_IDX) {
            if (ArrayUtils.getElementAt(children, parent).left == node) {
                ArrayUtils.getElementAt(children, parent).left = left;
            } else {
                ArrayUtils.getElementAt(children, parent).right = left;
            }
        } else {
            rootIndex = left;
        }
        setParent(left, parent);
        deleteBalance(left, ZERO);
    }

    private void noLeftChild(int node, int right) {
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);
        if (right == INVALID_IDX) {
            if (parent != INVALID_IDX) {
                if (ArrayUtils.getElementAt(children, parent).left == node) {
                    ArrayUtils.getElementAt(children, parent).left = INVALID_IDX;
                    deleteBalance(parent, MINUS_ONE);
                } else {
                    ArrayUtils.getElementAt(children, parent).right = INVALID_IDX;
                    deleteBalance(parent, ONE);
                }
            } else {
                rootIndex = INVALID_IDX;
            }
        } else {
            if (parent != INVALID_IDX) {
                if (ArrayUtils.getElementAt(children, parent).left == node) {
                    ArrayUtils.getElementAt(children, parent).left = right;
                } else {
                    ArrayUtils.getElementAt(children, parent).right = right;
                }
            } else {
                rootIndex = right;
            }
            setParent(right, parent);
            deleteBalance(right, ZERO);
        }
    }

    public int floorValue(int key) {
        int candidate = INVALID_IDX;
        for (int i = rootIndex; i != INVALID_IDX; ) {
            if (key < ArrayUtils.getElementAt(keys, i)) {
                i = ArrayUtils.getElementAt(children, i).left;
            } else if (key == ArrayUtils.getElementAt(keys, i)) {
                // found key
                return ArrayUtils.getElementAt(values, i);
            } else {
                candidate = i;
                i = ArrayUtils.getElementAt(children, i).right;
            }
        }
        if (candidate == INVALID_IDX) {
            return INVALID_IDX;
        } else {
            return ArrayUtils.getElementAt(values, candidate);
        }
    }

    private int findNode(int key) {
        for (int i = rootIndex; i != INVALID_IDX; ) {
            if (key < ArrayUtils.getElementAt(keys, i)) {
                i = ArrayUtils.getElementAt(children, i).left;
            } else if (key == ArrayUtils.getElementAt(keys, i)) {
                // found key
                return i;
            } else {
                i = ArrayUtils.getElementAt(children, i).right;
            }
        }
        return INVALID_IDX;
    }

    private void insertBalance(int node, byte balance) {
        int workingNode = node;
        byte workingBalance = balance;
        while (workingNode != INVALID_IDX) {
            workingBalance = (byte) (ArrayUtils.getElementAt(balances, workingNode) + workingBalance);
            ArrayUtils.putElementAt(balances, workingNode, workingBalance);

            if (workingBalance == ZERO) {
                break;
            } else if (workingBalance == REBALANCE_THRESHOLD) {
                if (ArrayUtils.getElementAt(balances, ArrayUtils.getElementAt(children, workingNode).left) == ONE) {
                    rotateRight(workingNode);
                } else {
                    rotateLeftRight(workingNode);
                }
                break;
            } else if (workingBalance == MINUS_REBALANCE_THRESHOLD) {
                if (ArrayUtils.getElementAt(balances, ArrayUtils.getElementAt(children, workingNode).right) == MINUS_ONE) {
                    rotateLeft(workingNode);
                } else {
                    rotateRightLeft(workingNode);
                }
                break;
            }

            final int parent = ArrayUtils.getElementAt(parentIndexes, workingNode);
            if (parent != INVALID_IDX) {
                workingBalance = ArrayUtils.getElementAt(children, parent).left == workingNode ? ONE : MINUS_ONE;
            }
            workingNode = parent;
        }
    }

    private void deleteBalance(int node, byte balance) {
        int workingNode = node;
        byte workingBalance = balance;
        while (workingNode != INVALID_IDX) {
            workingBalance = (byte) (ArrayUtils.getElementAt(balances, workingNode) + workingBalance);
            ArrayUtils.putElementAt(balances, workingNode, workingBalance);

            if (workingBalance == MINUS_REBALANCE_THRESHOLD) {
                if (ArrayUtils.getElementAt(balances, ArrayUtils.getElementAt(children, workingNode).right) <= ZERO) {
                    workingNode = rotateLeft(workingNode);
                    if (ArrayUtils.getElementAt(balances, workingNode) == ONE) {
                        break;
                    }
                } else {
                    workingNode = rotateRightLeft(workingNode);
                }
            } else if (workingBalance == REBALANCE_THRESHOLD) {
                if (ArrayUtils.getElementAt(balances, ArrayUtils.getElementAt(children, workingNode).left) >= ZERO) {
                    workingNode = rotateRight(workingNode);
                    if (ArrayUtils.getElementAt(balances, workingNode) == MINUS_ONE) {
                        break;
                    }
                } else {
                    workingNode = rotateLeftRight(workingNode);
                }
            } else if (workingBalance != ZERO) {
                break;
            }

            if (workingNode != INVALID_IDX) {
                final int parent = ArrayUtils.getElementAt(parentIndexes, workingNode);
                if (parent != INVALID_IDX) {
                    workingBalance = ArrayUtils.getElementAt(children, parent).left == workingNode ? MINUS_ONE : ONE;
                }
                workingNode = parent;
            }
        }
    }

    private int rotateLeft(int node) {
        final int right = ArrayUtils.getElementAt(children, node).right;
        final int rightLeft = ArrayUtils.getElementAt(children, right).left;
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);

        setParent(right, parent);
        setParent(node, right);
        setParent(rightLeft, node);
        ArrayUtils.getElementAt(children, right).left = node;
        ArrayUtils.getElementAt(children, node).right = rightLeft;

        if (node == rootIndex) {
            rootIndex = right;
        } else if (ArrayUtils.getElementAt(children, parent).right == node) {
            ArrayUtils.getElementAt(children, parent).right = right;
        } else {
            ArrayUtils.getElementAt(children, parent).left = right;
        }

        ArrayUtils.putElementAt(balances, right, (byte) (ArrayUtils.getElementAt(balances, right) + 1));
        ArrayUtils.putElementAt(balances, node, (byte) -ArrayUtils.getElementAt(balances, right));

        return right;
    }

    private int rotateRight(int node) {
        final int left = ArrayUtils.getElementAt(children, node).left;
        final int leftRight = ArrayUtils.getElementAt(children, left).right;
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);

        setParent(left, parent);
        setParent(node, left);
        setParent(leftRight, node);
        ArrayUtils.getElementAt(children, left).right = node;
        ArrayUtils.getElementAt(children, node).left = leftRight;

        if (node == rootIndex) {
            rootIndex = left;
        } else if (ArrayUtils.getElementAt(children, parent).left == node) {
            ArrayUtils.getElementAt(children, parent).left = left;
        } else {
            ArrayUtils.getElementAt(children, parent).right = left;
        }

        ArrayUtils.putElementAt(balances, left, (byte) (ArrayUtils.getElementAt(balances, left) - 1));
        ArrayUtils.putElementAt(balances, node, (byte) -ArrayUtils.getElementAt(balances, left));

        return left;
    }


    private int rotateLeftRight(int node) {
        final int left = ArrayUtils.getElementAt(children, node).left;
        final int leftRight = ArrayUtils.getElementAt(children, left).right;
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);
        setParent(leftRight, parent);
        setParent(left, leftRight);
        setParent(node, leftRight);
        final int leftRightRight = ArrayUtils.getElementAt(children, leftRight).right;
        setParent(leftRightRight, node);
        final int leftRightLeft = ArrayUtils.getElementAt(children, leftRight).left;
        setParent(leftRightLeft, left);
        ArrayUtils.getElementAt(children, node).left = leftRightRight;
        ArrayUtils.getElementAt(children, left).right = leftRightLeft;
        ArrayUtils.getElementAt(children, leftRight).left = left;
        ArrayUtils.getElementAt(children, leftRight).right = node;

        if (node == rootIndex) {
            rootIndex = leftRight;
        } else if (ArrayUtils.getElementAt(children, parent).left == node) {
            ArrayUtils.getElementAt(children, parent).left = leftRight;
        } else {
            ArrayUtils.getElementAt(children, parent).right = leftRight;
        }

        if (ArrayUtils.getElementAt(balances, leftRight) == ZERO) {
            ArrayUtils.putElementAt(balances, node, ZERO);
            ArrayUtils.putElementAt(balances, left, ZERO);
        } else if (ArrayUtils.getElementAt(balances, leftRight) == MINUS_ONE) {
            ArrayUtils.putElementAt(balances, node, ZERO);
            ArrayUtils.putElementAt(balances, left, ONE);
        } else {
            ArrayUtils.putElementAt(balances, node, MINUS_ONE);
            ArrayUtils.putElementAt(balances, left, ZERO);
        }
        ArrayUtils.putElementAt(balances, leftRight, ZERO);

        return leftRight;
    }


    private int rotateRightLeft(int node) {
        final int right = ArrayUtils.getElementAt(children, node).right;
        final int rightLeft = ArrayUtils.getElementAt(children, right).left;
        final int parent = ArrayUtils.getElementAt(parentIndexes, node);
        setParent(rightLeft, parent);
        setParent(right, rightLeft);
        setParent(node, rightLeft);
        final int rightLeftLeft = ArrayUtils.getElementAt(children, rightLeft).left;
        setParent(rightLeftLeft, node);
        final int rightLeftRight = ArrayUtils.getElementAt(children, rightLeft).right;
        setParent(rightLeftRight, right);
        ArrayUtils.getElementAt(children, node).right = rightLeftLeft;
        ArrayUtils.getElementAt(children, right).left = rightLeftRight;
        ArrayUtils.getElementAt(children, rightLeft).right = right;
        ArrayUtils.getElementAt(children, rightLeft).left = node;

        if (node == rootIndex) {
            rootIndex = rightLeft;
        } else if (ArrayUtils.getElementAt(children, parent).right == node) {
            ArrayUtils.getElementAt(children, parent).right = rightLeft;
        } else {
            ArrayUtils.getElementAt(children, parent).left = rightLeft;
        }

        if (ArrayUtils.getElementAt(balances, rightLeft) == ZERO) {
            ArrayUtils.putElementAt(balances, node, ZERO);
            ArrayUtils.putElementAt(balances, right, ZERO);
        } else if (ArrayUtils.getElementAt(balances, rightLeft) == ONE) {
            ArrayUtils.putElementAt(balances, node, ZERO);
            ArrayUtils.putElementAt(balances, right, MINUS_ONE);
        } else {
            ArrayUtils.putElementAt(balances, node, ONE);
            ArrayUtils.putElementAt(balances, right, ZERO);
        }
        ArrayUtils.putElementAt(balances, rightLeft, ZERO);

        return rightLeft;
    }

    private void setParent(int node, int parent) {
        if (node != INVALID_IDX) {
            ArrayUtils.putElementAt(parentIndexes, node, parent);
        }
    }

    public int size() {
        return size;
    }

    private static final class Node {
        private int left = INVALID_IDX;
        private int right = INVALID_IDX;

        private void reset() {
            left = INVALID_IDX;
            right = INVALID_IDX;
        }

        public void copyFrom(Node node) {
            this.left = node.left;
            this.right = node.right;
        }
    }
}