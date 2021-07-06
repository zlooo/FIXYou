package io.github.zlooo.fixyou.commons;

public class IntIntAVLTreeBase {

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

    public IntIntAVLTreeBase(int size) {
        this.parentIndexes = new int[size];
        this.keys = new int[size];
        this.values = new int[size];
        this.balances = new byte[size];
        this.children = new Node[size]; //maybe child should just be int[2*size]?
        for (int i = 0; i < size; i++) {
            children[i] = new Node();
        }
        this.maxSize = size;
    }

    public int get(int key) {
        final int node = findNode(key);
        if (node == INVALID_IDX) {
            return INVALID_IDX;
        } else {
            return values[node];
        }
    }

    public boolean put(int key, int val) {
        if (rootIndex == INVALID_IDX) {
            keys[size] = key;
            values[size] = val;
            balances[size] = ZERO;
            children[size].reset();
            setParent(size, INVALID_IDX);
            rootIndex = size++;
            return true;
        }
        boolean insertResult = false;
        for (int i = rootIndex; i != INVALID_IDX; i = (key < keys[i]) ? children[i].left : children[i].right) {
            if (key < keys[i]) {
                if (children[i].left == INVALID_IDX) {
                    if (size >= maxSize) {
                        break;
                    }
                    keys[size] = key;
                    values[size] = val;
                    balances[size] = ZERO;
                    children[size].reset();
                    setParent(size, i);
                    children[i].left = size++;
                    insertBalance(i, ONE);
                    insertResult = true;
                    break;
                }
            } else if (keys[i] == key) {
                values[i] = val;
                insertResult = true;
                break;
            } else {
                if (children[i].right == INVALID_IDX) {
                    if (size >= maxSize) {
                        break;
                    }
                    keys[size] = key;
                    values[size] = val;
                    balances[size] = ZERO;
                    children[size].reset();
                    setParent(size, i);
                    children[i].right = size++;
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
            removedValue = values[node];
        }

        final int left = children[node].left;
        final int right = children[node].right;

        if (left == INVALID_IDX) {
            noLeftChild(node, right);
        } else if (right == INVALID_IDX) {
            noRightChild(node, left);
        } else {
            int successor = right;
            if (children[successor].left == INVALID_IDX) {
                noLeftSuccessor(node, left, successor);
            } else {
                while (children[successor].left != INVALID_IDX) {
                    successor = children[successor].left;
                }

                final int parent = parentIndexes[node];
                final int successorParent = parentIndexes[successor];
                final int successorRight = children[successor].right;

                if (children[successorParent].left == successor) {
                    children[successorParent].left = successorRight;
                } else {
                    children[successorParent].right = successorRight;
                }

                setParent(successorRight, successorParent);
                setParent(successor, parent);
                setParent(right, successor);
                setParent(left, successor);
                children[successor].left = left;
                children[successor].right = right;
                balances[successor] = balances[node];

                if (node == rootIndex) {
                    rootIndex = successor;
                } else {
                    if (children[parent].left == node) {
                        children[parent].left = successor;
                    } else {
                        children[parent].right = successor;
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
            parent = parentIndexes[nodeFrom];
            if (children[parent].left == nodeFrom) {
                children[parent].left = nodeTo;
            } else {
                children[parent].right = nodeTo;
            }
        }

        // correct childs parent
        setParent(children[nodeFrom].left, nodeTo);
        setParent(children[nodeFrom].right, nodeTo);

        // move content
        keys[nodeTo] = keys[nodeFrom];
        values[nodeTo] = values[nodeFrom];
        balances[nodeTo] = balances[nodeFrom];
        children[nodeTo].copyFrom(children[nodeFrom]);
        setParent(nodeTo, parent);
    }

    private void noLeftSuccessor(int node, int left, int successor) {
        final int parent = parentIndexes[node];
        children[successor].left = left;
        balances[successor] = balances[node];
        setParent(successor, parent);
        setParent(left, successor);

        if (node == rootIndex) {
            rootIndex = successor;
        } else {
            if (children[parent].left == node) {
                children[parent].left = successor;
            } else {
                children[parent].right = successor;
            }
        }
        deleteBalance(successor, ONE);
    }

    private void noRightChild(int node, int left) {
        final int parent = parentIndexes[node];
        if (parent != INVALID_IDX) {
            if (children[parent].left == node) {
                children[parent].left = left;
            } else {
                children[parent].right = left;
            }
        } else {
            rootIndex = left;
        }
        setParent(left, parent);
        deleteBalance(left, ZERO);
    }

    private void noLeftChild(int node, int right) {
        final int parent = parentIndexes[node];
        if (right == INVALID_IDX) {
            if (parent != INVALID_IDX) {
                if (children[parent].left == node) {
                    children[parent].left = INVALID_IDX;
                    deleteBalance(parent, MINUS_ONE);
                } else {
                    children[parent].right = INVALID_IDX;
                    deleteBalance(parent, ONE);
                }
            } else {
                rootIndex = INVALID_IDX;
            }
        } else {
            if (parent != INVALID_IDX) {
                if (children[parent].left == node) {
                    children[parent].left = right;
                } else {
                    children[parent].right = right;
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
            if (key < keys[i]) {
                i = children[i].left;
            } else if (key == keys[i]) {
                // found key
                return values[i];
            } else {
                candidate = i;
                i = children[i].right;
            }
        }
        if (candidate == INVALID_IDX) {
            return INVALID_IDX;
        } else {
            return values[candidate];
        }
    }

    private int findNode(int key) {
        for (int i = rootIndex; i != INVALID_IDX; ) {
            if (key < keys[i]) {
                i = children[i].left;
            } else if (key == keys[i]) {
                // found key
                return i;
            } else {
                i = children[i].right;
            }
        }
        return INVALID_IDX;
    }

    private void insertBalance(int node, byte balance) {
        int workingNode = node;
        byte workingBalance = balance;
        while (workingNode != INVALID_IDX) {
            workingBalance = (byte) (balances[workingNode] + workingBalance);
            balances[workingNode] = workingBalance;

            if (workingBalance == ZERO) {
                break;
            } else if (workingBalance == REBALANCE_THRESHOLD) {
                if (balances[children[workingNode].left] == ONE) {
                    rotateRight(workingNode);
                } else {
                    rotateLeftRight(workingNode);
                }
                break;
            } else if (workingBalance == MINUS_REBALANCE_THRESHOLD) {
                if (balances[children[workingNode].right] == MINUS_ONE) {
                    rotateLeft(workingNode);
                } else {
                    rotateRightLeft(workingNode);
                }
                break;
            }

            final int parent = parentIndexes[workingNode];
            if (parent != INVALID_IDX) {
                workingBalance = children[parent].left == workingNode ? ONE : MINUS_ONE;
            }
            workingNode = parent;
        }
    }

    private void deleteBalance(int node, byte balance) {
        int workingNode = node;
        byte workingBalance = balance;
        while (workingNode != INVALID_IDX) {
            workingBalance = (byte) (balances[workingNode] + workingBalance);
            balances[workingNode] = workingBalance;

            if (workingBalance == MINUS_REBALANCE_THRESHOLD) {
                if (balances[children[workingNode].right] <= ZERO) {
                    workingNode = rotateLeft(workingNode);
                    if (balances[workingNode] == ONE) {
                        break;
                    }
                } else {
                    workingNode = rotateRightLeft(workingNode);
                }
            } else if (workingBalance == REBALANCE_THRESHOLD) {
                if (balances[children[workingNode].left] >= ZERO) {
                    workingNode = rotateRight(workingNode);
                    if (balances[workingNode] == MINUS_ONE) {
                        break;
                    }
                } else {
                    workingNode = rotateLeftRight(workingNode);
                }
            } else if (workingBalance != ZERO) {
                break;
            }

            if (workingNode != INVALID_IDX) {
                final int parent = parentIndexes[workingNode];
                if (parent != INVALID_IDX) {
                    workingBalance = children[parent].left == workingNode ? MINUS_ONE : ONE;
                }
                workingNode = parent;
            }
        }
    }

    private int rotateLeft(int node) {
        final int right = children[node].right;
        final int rightLeft = children[right].left;
        final int parent = parentIndexes[node];

        setParent(right, parent);
        setParent(node, right);
        setParent(rightLeft, node);
        children[right].left = node;
        children[node].right = rightLeft;

        if (node == rootIndex) {
            rootIndex = right;
        } else if (children[parent].right == node) {
            children[parent].right = right;
        } else {
            children[parent].left = right;
        }

        balances[right]++;
        balances[node] = (byte) -balances[right];

        return right;
    }

    private int rotateRight(int node) {
        final int left = children[node].left;
        final int leftRight = children[left].right;
        final int parent = parentIndexes[node];

        setParent(left, parent);
        setParent(node, left);
        setParent(leftRight, node);
        children[left].right = node;
        children[node].left = leftRight;

        if (node == rootIndex) {
            rootIndex = left;
        } else if (children[parent].left == node) {
            children[parent].left = left;
        } else {
            children[parent].right = left;
        }

        balances[left]--;
        balances[node] = (byte) -balances[left];

        return left;
    }


    private int rotateLeftRight(int node) {
        final int left = children[node].left;
        final int leftRight = children[left].right;
        final int parent = parentIndexes[node];
        setParent(leftRight, parent);
        setParent(left, leftRight);
        setParent(node, leftRight);
        final int leftRightRight = children[leftRight].right;
        setParent(leftRightRight, node);
        final int leftRightLeft = children[leftRight].left;
        setParent(leftRightLeft, left);
        children[node].left = leftRightRight;
        children[left].right = leftRightLeft;
        children[leftRight].left = left;
        children[leftRight].right = node;

        if (node == rootIndex) {
            rootIndex = leftRight;
        } else if (children[parent].left == node) {
            children[parent].left = leftRight;
        } else {
            children[parent].right = leftRight;
        }

        if (balances[leftRight] == ZERO) {
            balances[node] = ZERO;
            balances[left] = ZERO;
        } else if (balances[leftRight] == MINUS_ONE) {
            balances[node] = ZERO;
            balances[left] = ONE;
        } else {
            balances[node] = MINUS_ONE;
            balances[left] = ZERO;
        }
        balances[leftRight] = ZERO;

        return leftRight;
    }


    private int rotateRightLeft(int node) {
        final int right = children[node].right;
        final int rightLeft = children[right].left;
        final int parent = parentIndexes[node];
        setParent(rightLeft, parent);
        setParent(right, rightLeft);
        setParent(node, rightLeft);
        final int rightLeftLeft = children[rightLeft].left;
        setParent(rightLeftLeft, node);
        final int rightLeftRight = children[rightLeft].right;
        setParent(rightLeftRight, right);
        children[node].right = rightLeftLeft;
        children[right].left = rightLeftRight;
        children[rightLeft].right = right;
        children[rightLeft].left = node;

        if (node == rootIndex) {
            rootIndex = rightLeft;
        } else if (children[parent].right == node) {
            children[parent].right = rightLeft;
        } else {
            children[parent].left = rightLeft;
        }

        if (balances[rightLeft] == ZERO) {
            balances[node] = ZERO;
            balances[right] = ZERO;
        } else if (balances[rightLeft] == ONE) {
            balances[node] = ZERO;
            balances[right] = MINUS_ONE;
        } else {
            balances[node] = ONE;
            balances[right] = ZERO;
        }
        balances[rightLeft] = ZERO;

        return rightLeft;
    }

    private void setParent(int node, int parent) {
        if (node != INVALID_IDX) {
            parentIndexes[node] = parent;
        }
    }

    public int size(){
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
