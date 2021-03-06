package nl.NG.Jetfightergame.Tools.DataStructures;

import java.util.*;
import java.util.function.Consumer;

/**
 * optimized version of {@code List<Pair<L, R>>} instances. This list does not permit null values
 * @author Geert van Ieperen created on 29-6-2018.
 */
public class PairList<L, R> extends AbstractList<Pair<L, R>> {
    private final List<L> leftList;
    private final List<R> rightList;

    public PairList(int expectedSize) {
        leftList = new ArrayList<>(expectedSize);
        rightList = new ArrayList<>(expectedSize);
    }

    public PairList() {
        leftList = new ArrayList<>();
        rightList = new ArrayList<>();
    }

    /** adds the given pair to the end of the list */
    public boolean add(Pair<L, R> p) {
        return add(p.left, p.right);
    }

    /** adds a pair to the end of the list */
    public boolean add(L left, R right) {
        leftList.add(left);
        rightList.add(right);
        return true;
    }

    @Override
    public void add(int index, Pair<L, R> element) {
        add(index, element.left, element.right);
    }

    public void add(int index, L left, R right) {
        leftList.add(index, left);
        rightList.add(index, right);
    }

    public void addAll(PairList<L, R> source) {
        leftList.addAll(source.leftList);
        rightList.addAll(source.rightList);
    }

    @Override
    public boolean addAll(Collection<? extends Pair<L, R>> c) {
        c.forEach(this::add);
        return true;
    }

    /** sets the element of the given position to the given pair */
    public Pair<L, R> set(int index, Pair<L, R> p) {
        Pair<L, R> old = get(index);
        set(index, p.left, p.right);
        return old;
    }

    @Override
    public Pair<L, R> remove(int index) {
        return new Pair<>(
                leftList.remove(index),
                rightList.remove(index)
        );
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            return -1;
        } else {
            if (o instanceof Pair) {
                Pair pair = (Pair) o;
                try {
                    Iterator<L> lit = leftList.iterator();
                    Iterator<R> rit = rightList.iterator();
                    for (int i = 0; i < size(); i++) {
                        if ((lit.next() == pair.left) && (rit.next() == pair.right)) {
                            return i;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException | NoSuchElementException ex) {
                    throw new ConcurrentModificationException();
                }
            }

            return -1;
        }
    }

    /** sets the element of the given position to the given pair */
    public void set(int index, L left, R right) {
        leftList.set(index, left);
        rightList.set(index, right);
    }

    /** @return the left value of the pair at the given nextIndex */
    public L left(int index) {
        return leftList.get(index);
    }

    /** @return the right value of the pair at the given nextIndex */
    public R right(int index) {
        return rightList.get(index);
    }

    public Pair<L, R> get(int index) {
        return new Pair<>(leftList.get(index), rightList.get(index));
    }

    @Override
    public int size() {
        return leftList.size();
    }

    @Override
    public boolean isEmpty() {
        return leftList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        return indexOf(o) != -1;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index == -1) return false;
        leftList.remove(index);
        rightList.remove(index);
        return true;
    }

    @Override
    public void clear() {
        leftList.clear();
        rightList.clear();
    }

    @Override
    public Spliterator<Pair<L, R>> spliterator() {
        return new PairSpliterator(0, size() - 1);
    }

    @Override
    public ListIterator<Pair<L, R>> listIterator(int index) {
        return new PairListIterator(index);
    }

    public void addAll(List<L> leftNew, List<R> rightNew) {
        if (leftNew.size() != rightNew.size())
            throw new IllegalArgumentException(
                    "Given lists are of unequal size: " + leftNew.size() + ", " + rightNew.size()
            );

        leftList.addAll(leftNew);
        rightList.addAll(rightNew);
    }

    /**
     * allows iteration over this pairList
     */
    private class PairListIterator implements ListIterator<Pair<L, R>> {
        private int nextIndex;
        private int initialSize;

        public PairListIterator(int start) {
            nextIndex = start;
            initialSize = size();
        }

        @Override
        public boolean hasNext() {
            return nextIndex < initialSize;
        }

        @Override
        public Pair<L, R> next() {
            return new Pair<>(leftList.get(nextIndex), rightList.get(nextIndex++));
        }

        @Override
        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        @Override
        public Pair<L, R> previous() {
            nextIndex -= 2;
            return new Pair<>(leftList.get(nextIndex), rightList.get(nextIndex));
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 2;
        }

        @Override
        public void remove() {
            leftList.remove(nextIndex - 1);
            rightList.remove(nextIndex - 1);
            initialSize--;
        }

        @Override
        public void set(Pair<L, R> pair) {
            leftList.set(nextIndex - 1, pair.left);
            rightList.set(nextIndex - 1, pair.right);
        }

        @Override
        public void add(Pair<L, R> pair) {
            PairList.this.add(nextIndex, pair);
        }
    }

    private class PairSpliterator implements Spliterator<Pair<L, R>> {
        int nextInd;
        int maxInd;

        public PairSpliterator(int startInd, int maxInd) {
            nextInd = startInd;
            this.maxInd = maxInd;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Pair<L, R>> action) {
            if (nextInd > maxInd) {
                return false;
            }

            Pair<L, R> p = get(nextInd++);
            action.accept(p);
            return true;
        }

        @Override
        public Spliterator<Pair<L, R>> trySplit() {
            int newMax = (maxInd - nextInd) / 2;
            int newIndex = nextInd;
            nextInd = newMax + 1;
            return new PairSpliterator(newIndex, newMax);
        }

        @Override
        public long estimateSize() {
            return (maxInd - nextInd) + 1;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | NONNULL | IMMUTABLE;
        }
    }

}
