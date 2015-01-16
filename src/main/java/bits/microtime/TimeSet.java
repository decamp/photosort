//package bits.microtime;
//
//import java.lang.reflect.Array;
//import java.util.*;
//
//
///**
// * Like a normal Set, but automatically merges and splits timeblocks as needed.
// *
// *
// * @author Philip DeCamp
// */
//public class TimeSet implements Set<TimeBlock> {
//
//    private Node mRoot = null;
//
//    private int mSize = 0;
//    private int mModCount = 0;
//
//
//    public TimeSet() {}
//
//
//
//
//    public boolean add(TimeBlock tb) {
//        if(tb == null)
//            return false;
//
//        return add(tb.getStartMicros(), tb.getStopMicros());
//    }
//
//
//    public boolean add(long startMicros, long stopMicros) {
//        if(stopMicros <= startMicros)
//            return false;
//
//        Node node = mRoot;
//
//        if(node == null) {
//            Node newNode = new Node(startMicros, stopMicros);
//            insertNode(newNode, null, false);
//            return true;
//        }
//
//
//        //Find highest-level node that intersects with time.
//        while(true) {
//            if(node.mStart > stopMicros) {
//                if(node.mLeft == null) {
//                    Node n = new Node(startMicros, stopMicros);
//                    insertNode(n, node, true);
//                    return true;
//                }
//
//                node = node.mLeft;
//
//            }else if(node.mStop < startMicros) {
//                if(node.mRight == null) {
//                    Node n = new Node(startMicros, stopMicros);
//                    insertNode(n, node, false);
//                    return true;
//                }
//
//                node = node.mRight;
//
//            }else if(node.mStart <= startMicros && node.mStop >= stopMicros) {
//                //Do nothing if TimeBlock is completely contained in set.
//                return false;
//
//            }else{
//                //Found intersection.
//                break;
//            }
//        }
//
//        //Find first node that intersects or the node just before that.
//        if(node.mLeft != null) {
//            node = node.mLeft;
//
//            while(true) {
//                if(node.mStart > stopMicros || node.mStop < startMicros) {
//                    //No intersection.  Go right.
//                    if(node.mRight == null)
//                        break;
//
//                    node = node.mRight;
//                }else{
//                    //No intersection.  Go left.
//                    if(node.mLeft == null)
//                        break;
//
//                    node = node.mLeft;
//                }
//            }
//        }
//
//        //Go forward to the first one that actually intersects.
//        while(node.mStop < startMicros)
//            node = nextNode(node);
//
//        //Remove intersecting nodes, updating the time bounds in the process.
//        while(node != null && node.mStart <= stopMicros) {
//
//            //Find the next node before deletions occur so that we don't lose our place.
//            Node next = nextNode(node);
//
//            //Expand bounds and delete node.
//            if(node.mStart < startMicros)
//                startMicros = node.mStart;
//
//            if(node.mStop > stopMicros)
//                stopMicros = node.mStop;
//
//            removeNode(node);
//            node = next;
//        }
//
//        //Find parent.
//        insertDisjoint(startMicros, stopMicros);
//        return true;
//    }
//
//
//    public boolean addAll(Collection<? extends TimeBlock> c) {
//        boolean ret = false;
//
//        for(TimeBlock tb: c) {
//            ret |= add(tb);
//        }
//
//        return ret;
//    }
//
//
//    public boolean remove(Object timeBlock) {
//        if(!(timeBlock instanceof TimeBlock))
//            return false;
//
//        TimeBlock tb = (TimeBlock)timeBlock;
//        return remove(tb.getStartMicros(), tb.getStopMicros());
//    }
//
//
//    public boolean remove(long startMicros, long stopMicros) {
//        if(stopMicros <= startMicros)
//            return false;
//
//        Node node = mRoot;
//
//        if(node == null)
//            return false;
//
//        //Find highest-level node that intersects with time.
//        while(true) {
//            if(node.mStart > stopMicros) {
//                if(node.mLeft == null)
//                    return false;
//
//                node = node.mLeft;
//
//            }else if(node.mStop < startMicros) {
//                if(node.mRight == null)
//                    return false;
//
//                node = node.mRight;
//
//            }else if(node.mStart <= startMicros && node.mStop >= stopMicros) {
//                //Portion to be removed lies entirely in one TimeBlock.
//                removeNode(node);
//                if(node.mStart < startMicros)
//                    insertDisjoint(node.mStart, startMicros);
//
//                if(stopMicros < node.mStop)
//                    insertDisjoint(stopMicros, node.mStop);
//
//                return true;
//
//            }else{
//                //Found intersection.
//                break;
//            }
//        }
//
//        //Find first node that intersects or the node just before that.
//        if(node.mLeft != null) {
//            node = node.mLeft;
//
//            while(true) {
//                if(node.mStart > stopMicros || node.mStop < startMicros) {
//                    //No intersection.  Go right.
//                    if(node.mRight == null)
//                        break;
//
//                    node = node.mRight;
//                }else{
//                    //No intersection.  Go left.
//                    if(node.mLeft == null)
//                        break;
//
//                    node = node.mLeft;
//                }
//            }
//        }
//
//        //Go forward to the first one that actually intersects.
//        while(node.mStop < startMicros)
//            node = nextNode(node);
//
//
//        //Remove or trim intersecting nodes.
//        while(node != null && node.mStart <= stopMicros) {
//
//            //Find the next node before deletions occur so that we don't lose our place.
//            Node next = nextNode(node);
//
//            removeNode(node);
//
//            if(node.mStart < startMicros) {
//                //Re-insert start of this timeblock.
//                insertDisjoint(node.mStart, startMicros);
//
//            }else if(stopMicros < node.mStop) {
//                //Re-insert end of this timeblock.
//                insertDisjoint(stopMicros, node.mStop);
//                break;
//            }
//
//            node = next;
//        }
//
//        return true;
//    }
//
//
//    public boolean removeAll(Collection<?> c) {
//        boolean ret = false;
//
//        for(Object obj: c) {
//            ret |= remove(obj);
//        }
//
//        return ret;
//    }
//
//
//    public boolean retainAll(Collection<?> c) {
//        throw new UnsupportedOperationException();
//    }
//
//
//    public void clear() {
//        mRoot = null;
//        mSize = 0;
//        mModCount++;
//    }
//
//
//    public boolean isEmpty() {
//        return mRoot == null;
//    }
//
//
//    public int size() {
//        return mSize;
//    }
//
//
//    public boolean contains(Object timeBlock) {
//        if(!(timeBlock instanceof TimeBlock))
//            return false;
//
//        TimeBlock tb = (TimeBlock)timeBlock;
//        return contains(tb.getStartMicros(), tb.getStopMicros());
//    }
//
//
//    public boolean contains(long timeMicro) {
//        Node node = mRoot;
//
//        while(node != null) {
//            if(timeMicro < node.mStart) {
//                node = node.mLeft;
//            }else if(timeMicro >= node.mStop) {
//                node = node.mRight;
//            }else{
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//
//    public boolean contains(long startMicros, long stopMicros) {
//        Node node = mRoot;
//
//        while(node != null) {
//            if(stopMicros <= node.mStart) {
//                node = node.mLeft;
//            }else if(startMicros >= node.mStop) {
//                node = node.mRight;
//            }
//
//            return node.mStart <= startMicros && node.mStop >= stopMicros;
//        }
//
//        return false;
//    }
//
//
//    public boolean containsAll(Collection<?> c) {
//        for(Object obj: c) {
//            if(!contains(obj))
//                return false;
//        }
//
//        return true;
//    }
//
//
//    public TimeBlock getContainingBlock(long timeMicro) {
//        Node node = mRoot;
//
//        while(node != null) {
//            if(timeMicro < node.mStart) {
//                node = node.mLeft;
//            }else if(timeMicro > node.mStop) {
//                node = node.mRight;
//            }else{
//                return TimeBlock.fromMicros(node.mStart, node.mStop);
//            }
//        }
//
//        return null;
//    }
//
//
//    public Iterator<TimeBlock> iterator() {
//        return new TimeIterator();
//    }
//
//
//    public List<TimeBlock> intersect(TimeBlock range) {
//        return intersect(range.getStartMicros(), range.getStopMicros());
//    }
//
//
//    public List<TimeBlock> intersect(long startMicros, long stopMicros) {
//        List<TimeBlock> ret = new ArrayList<TimeBlock>();
//
//        Node node = mRoot;
//
//        {
//            Node first = null;
//
//            //Find first overlapping node.
//            while(node != null) {
//                if(stopMicros <= node.mStart) {
//                    node = node.mLeft;
//                }else if(startMicros >= node.mStop) {
//                    node = node.mRight;
//                }else{
//                    first = node;
//                    node = node.mLeft;
//                }
//            }
//
//            node = first;
//        }
//
//        while(node != null && node.mStart <= stopMicros) {
//            long interStart = Math.max(node.mStart, startMicros);
//            long interStop = Math.min(node.mStop, stopMicros);
//
//            ret.add(TimeBlock.fromMicros(interStart, interStop));
//            node = nextNode(node);
//        }
//
//        return ret;
//    }
//
//
//    public List<TimeBlock> subtractFrom(TimeBlock range) {
//        return subtractFrom(range.getStartMicros(), range.getStopMicros());
//    }
//
//
//    public List<TimeBlock> subtractFrom(long startMicros, long stopMicros) {
//        List<TimeBlock> ret = new ArrayList<TimeBlock>();
//
//        Node node = mRoot;
//
//        {
//            Node first = null;
//
//            //Find first overlapping node.
//            while(node != null) {
//                if(stopMicros <= node.mStart) {
//                    node = node.mLeft;
//                }else if(startMicros >= node.mStop) {
//                    node = node.mRight;
//                }else{
//                    first = node;
//                    node = node.mLeft;
//                }
//            }
//
//            node = first;
//        }
//
//        if(node == null) {
//            ret.add(TimeBlock.fromMicros(startMicros, stopMicros));
//            return ret;
//        }
//
//        long lastStop = startMicros;
//
//        while(node != null && node.mStart <= stopMicros) {
//            if(node.mStart > lastStop) {
//                ret.add(TimeBlock.fromMicros(lastStop, node.mStart));
//            }
//
//            lastStop = node.mStop;
//            node = nextNode(node);
//        }
//
//        if(lastStop < stopMicros) {
//            ret.add(TimeBlock.fromMicros(lastStop, stopMicros));
//        }
//
//        return ret;
//    }
//
//
//    public TimeBlock[] toArray() {
//        TimeBlock[] ret = new TimeBlock[mSize];
//        Node node = firstNode();
//
//        for(int i = 0; i < ret.length; i++) {
//            ret[i] = TimeBlock.fromMicros(node.mStart, node.mStop);
//            node = nextNode(node);
//        }
//
//        return ret;
//    }
//
//
//    @SuppressWarnings("unchecked")
//    public <T> T[] toArray(T[] a) {
//        Class<?> c = a.getClass().getComponentType();
//        if(!c.isAssignableFrom(TimeBlock.class))
//            throw new ArrayStoreException();
//
//        if(a.length < mSize) {
//            a = (T[])Array.newInstance(a.getClass().getComponentType(), mSize);
//        }else if(a.length > mSize) {
//            a[mSize] = null;
//        }
//
//        Node node = firstNode();
//
//        for(int i = 0; i < mSize; i++) {
//            a[i] = (T)TimeBlock.fromMicros(node.mStart, node.mStop);
//            node = nextNode(node);
//        }
//
//        return a;
//    }
//
//
//
//
//    private void insertDisjoint(long startMicros, long stopMicros) {
//        Node newNode = new Node(startMicros, stopMicros);
//        Node node = mRoot;
//
//        if(node == null) {
//            insertNode(newNode, node, false);
//            return;
//        }
//
//        while(true) {
//            if(node.mStart < startMicros) {
//                if(node.mRight == null) {
//                    insertNode(newNode, node, false);
//                    return;
//                }
//
//                node = node.mRight;
//
//            }else{
//                if(node.mLeft == null) {
//                    insertNode(newNode, node, true);
//                    return;
//                }
//
//                node = node.mLeft;
//            }
//        }
//    }
//
//
//
//
//    /*************************************************************
//     * Fundamental Red-Black Tree Operations
//     *
//     * These operations do not look at the data portion (mStart & mStop) of each
//     * node, and thus may be easily transferred to other classes.  These methods
//     * only rely on two member variables: mRoot, mSize and mModCount.
//     *************************************************************/
//
//    private static final boolean BLACK = false;
//    private static final boolean RED = true;
//
//
//
//    private void insertNode(Node node, Node parent, boolean left) {
//        mSize++;
//        mModCount++;
//
//        if(parent == null) {
//            mRoot = node;
//            node.mColor = BLACK;
//            return;
//        }
//
//        node.mParent = parent;
//
//        if(left) {
//            parent.mLeft = node;
//        }else{
//            parent.mRight = node;
//        }
//
//        while(true) {
//            if(parent == null) {
//                node.mColor = BLACK;
//                return;
//            }
//
//            node.mColor = RED;
//
//            if(parent.mColor == BLACK) {
//                return;
//            }
//
//            Node grandParent = parent.mParent;
//            Node uncle = (grandParent.mLeft == parent ? grandParent.mRight: grandParent.mLeft);
//
//            if(uncle != null && uncle.mColor == RED) {
//                parent.mColor = BLACK;
//                uncle.mColor = BLACK;
//                grandParent.mColor = RED;
//
//                node = grandParent;
//                parent = grandParent.mParent;
//                left = (parent == null || parent.mLeft == node);
//
//                continue;
//            }
//
//
//            if(!left && parent == grandParent.mLeft) {
//                rotateLeft(parent);
//                parent = node;
//                node = parent.mLeft;
//                left = true;
//
//            }else if(left && parent == grandParent.mRight) {
//                rotateRight(parent);
//                parent = node;
//                node = parent.mRight;
//                left = false;
//            }
//
//            parent.mColor = BLACK;
//            grandParent.mColor = RED;
//
//            if(left) {
//                rotateRight(grandParent);
//            }else{
//                rotateLeft(grandParent);
//            }
//
//            break;
//        }
//    }
//
//
//    private void removeNode(Node node) {
//        mSize--;
//        mModCount++;
//
//        //If we are deleting a node with two children, swap
//        //it with a node that has at most one child.
//        if(node.mLeft != null && node.mRight != null) {
//            Node swapNode = node.mLeft;
//            while(swapNode.mRight != null)
//                swapNode = swapNode.mRight;
//
//            swapNodes(node, swapNode);
//        }
//
//        //We are now guaranteed that node has no more than one non-null child.
//        //We now relabel the node to be deleted "oldParent", the parent of that
//        //    deletion node "newParent", and it's child "node".
//        Node oldParent = node;
//        Node newParent = node.mParent;
//
//        node = (node.mLeft == null ? node.mRight: node.mLeft);
//
//        //Set parent of child node to be newParent.
//        if(node != null)
//            node.mParent = newParent;
//
//        //Set child of newParent to node.
//        if(newParent == null) {
//            mRoot = node;
//
//        }else{
//            //left = newParent.mLeft == oldParent;
//            if(newParent.mLeft == oldParent) {
//                newParent.mLeft = node;
//            }else{
//                newParent.mRight = node;
//            }
//        }
//
//        //If oldParent was RED, the constraints will be maintained.
//        if(oldParent.mColor == RED)
//            return;
//
//        //If the oldParent is BLACK and the node is RED, we swap colors.
//        if(node != null && node.mColor == RED) {
//            node.mColor = BLACK;
//            return;
//        }
//
//        //If both oldParent and child are black, we're in a world of pain and
//        //must rebalance the tree.
//        while(true) {
//
//            //Case 1: node is new root.  We're done.
//            if(newParent == null)
//                return;
//
//            //Case 2: Sibling is RED.  Reverse newParent and sibling colors and
//            //rotate at newParent.  (If tree was balanced before,
//            //sibling is guaranteed to be non-null.)
//            boolean left = node == newParent.mLeft;
//            Node sibling = left ? newParent.mRight : newParent.mLeft;
//
//            if(sibling.mColor == RED) {
//                newParent.mColor = RED;
//                sibling.mColor = BLACK;
//
//                if(left) {
//                    rotateLeft(newParent);
//                    sibling = newParent.mRight;
//                }else{
//                    rotateRight(newParent);
//                    sibling = newParent.mLeft;
//                }
//            }
//
//
//            if((sibling.mLeft == null || sibling.mLeft.mColor == BLACK) &&
//               (sibling.mRight == null || sibling.mRight.mColor == BLACK))
//            {
//                if(newParent.mColor == BLACK) {
//                    //Case 3: newParent, sibling, and sibling's children are black.
//                    //Repaint sibling red and reiterate through loop.
//                    sibling.mColor = RED;
//                    node = newParent;
//                    newParent = node.mParent;
//                    continue;
//                }else{
//                    //Case 4: sibling and sibling's children are black, but
//                    //newParent is red.  In this case, swap colors between
//                    //newParent and sibling.
//                    sibling.mColor = RED;
//                    newParent.mColor = BLACK;
//                    return;
//                }
//            }
//
//            //Case 5: sibling is black but has at least one red child.
//            //Here we perform a series of rotations to balance out the tree.
//            if(left) {
//                if(sibling.mRight == null || sibling.mRight.mColor == BLACK) {
//                    rotateRight(sibling);
//                    sibling = sibling.mParent;
//                }
//
//                sibling.mColor = newParent.mColor;
//                sibling.mRight.mColor = BLACK;
//                rotateLeft(newParent);
//
//            }else{
//                if(sibling.mLeft == null || sibling.mLeft.mColor == BLACK) {
//                    rotateLeft(sibling);
//                    sibling = sibling.mParent;
//                }
//
//                sibling.mColor = newParent.mColor;
//                sibling.mLeft.mColor = BLACK;
//                rotateRight(newParent);
//
//            }
//
//            newParent.mColor = BLACK;
//            break;
//        }
//    }
//
//
//    private void rotateLeft(Node node) {
//        Node right = node.mRight;
//        if(right == null)
//            return;
//
//        node.mRight = right.mLeft;
//        if(node.mRight != null)
//            node.mRight.mParent = node;
//
//        right.mLeft = node;
//
//        if(node == mRoot) {
//            mRoot = right;
//            right.mParent = null;
//            node.mParent = right;
//        }else{
//            right.mParent = node.mParent;
//            node.mParent = right;
//
//            if(node == right.mParent.mLeft) {
//                right.mParent.mLeft = right;
//            }else{
//                right.mParent.mRight = right;
//            }
//        }
//    }
//
//
//    private void rotateRight(Node node) {
//        Node left = node.mLeft;
//        if(left == null)
//            return;
//
//        node.mLeft = left.mRight;
//        left.mRight = node;
//
//        if(node.mLeft != null)
//            node.mLeft.mParent = node;
//
//        if(node == mRoot) {
//            mRoot = left;
//            left.mParent = null;
//            node.mParent = left;
//        }else{
//            left.mParent = node.mParent;
//            node.mParent = left;
//
//            if(node == left.mParent.mRight) {
//                left.mParent.mRight = left;
//            }else{
//                left.mParent.mLeft = left;
//            }
//        }
//    }
//
//
//    private Node firstNode() {
//        if(mRoot == null)
//            return null;
//
//        Node node = mRoot;
//        while(node.mLeft != null)
//            node = node.mLeft;
//
//        return node;
//    }
//
//
//    private Node nextNode(Node node) {
//        if(node.mRight != null) {
//            node = node.mRight;
//
//            while(node.mLeft != null)
//                node = node.mLeft;
//
//        }else{
//            while(node.mParent != null && node.mParent.mRight == node)
//                node = node.mParent;
//
//            node = node.mParent;
//        }
//
//        return node;
//    }
//
//
//    private void swapNodes(Node a, Node b) {
//
//        if(a.mParent == b) {
//            swapNodes(b, a);
//            return;
//        }
//
//        {
//            boolean tempColor = a.mColor;
//            a.mColor = b.mColor;
//            b.mColor = tempColor;
//        }
//
//        Node tempNode;
//
//        if(a.mLeft == b) {
//
//            a.mLeft = b.mLeft;
//            b.mLeft = a;
//            if(a.mLeft != null)
//                a.mLeft.mParent = a;
//
//            tempNode = a.mRight;
//            a.mRight = b.mRight;
//            b.mRight = tempNode;
//            if(a.mRight != null)
//                a.mRight.mParent = a;
//            if(b.mRight != null)
//                b.mRight.mParent = b;
//
//            b.mParent = a.mParent;
//            a.mParent = b;
//
//            if(b.mParent == null) {
//                mRoot = b;
//            }else if(b.mParent.mLeft == a) {
//                b.mParent.mLeft = b;
//            }else{
//                b.mParent.mRight = b;
//            }
//
//        }else if(a.mRight == b) {
//            a.mRight = b.mRight;
//            b.mRight = a;
//            if(a.mRight != null)
//                a.mRight.mParent = a;
//
//            tempNode = a.mLeft;
//            a.mLeft = b.mLeft;
//            b.mLeft = tempNode;
//            if(a.mLeft != null)
//                a.mLeft.mParent = a;
//            if(b.mLeft != null)
//                b.mLeft.mParent = b;
//
//            b.mParent = a.mParent;
//            a.mParent = b;
//
//            if(b.mParent == null) {
//                mRoot = b;
//            }else if(b.mParent.mLeft == a) {
//                b.mParent.mLeft = b;
//            }else{
//                b.mParent.mRight = b;
//            }
//
//        }else{
//            tempNode = a.mLeft;
//            a.mLeft = b.mLeft;
//            b.mLeft = tempNode;
//            if(a.mLeft != null)
//                a.mLeft.mParent = a;
//            if(b.mLeft != null)
//                b.mLeft.mParent = b;
//
//            tempNode = a.mRight;
//            a.mRight = b.mRight;
//            b.mRight = tempNode;
//            if(a.mRight != null)
//                a.mRight.mParent = a;
//            if(b.mRight != null)
//                b.mRight.mParent = b;
//
//            tempNode = a.mParent;
//            a.mParent = b.mParent;
//            b.mParent = tempNode;
//
//            if(a.mParent == null) {
//                mRoot = a;
//            }else if(a.mParent.mLeft == b) {
//                a.mParent.mLeft = a;
//            }else{
//                a.mParent.mRight = a;
//            }
//
//            if(b.mParent == null) {
//                mRoot = b;
//            }else if(b.mParent.mLeft == a) {
//                b.mParent.mLeft = b;
//            }else{
//                b.mParent.mRight = b;
//            }
//        }
//    }
//
//
//
//
//    private static class Node {
//        public final long mStart;
//        public final long mStop;
//
//        public boolean mColor = RED;
//        public Node mParent = null;
//        public Node mLeft = null;
//        public Node mRight = null;
//
//
//        public Node(long start, long stop) {
//            mStart = start;
//            mStop = stop;
//        }
//
//    }
//
//
//
//
//
//    /*********************************
//     * View classes
//     *********************************/
//
//
//    private class TimeIterator implements Iterator<TimeBlock> {
//
//        private int mmModCount = mModCount;
//        private Node mPrev = null;
//        private Node mNext = firstNode();
//
//        public boolean hasNext() {
//            return mNext != null;
//        }
//
//        public TimeBlock next() {
//            if(mNext == null)
//                throw new NoSuchElementException();
//
//            if(mModCount != mmModCount)
//                throw new ConcurrentModificationException();
//
//            TimeBlock ret = TimeBlock.fromMicros(mNext.mStart, mNext.mStop);
//            mPrev = mNext;
//            mNext = nextNode(mNext);
//
//            return ret;
//        }
//
//        public void remove() {
//            if(mPrev == null)
//                throw new IllegalStateException();
//
//            if(mModCount != mmModCount)
//                throw new ConcurrentModificationException();
//
//            removeNode(mPrev);
//            mPrev = null;
//
//            mmModCount = mModCount;
//        }
//
//    }
//
//}
