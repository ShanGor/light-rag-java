package cn.gzten.rag.data.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class NullablePair <L, R>{
    private L left;
    private R right;

    /**
     * It will sort the 2 elements in the pair, if left > right, then swap them.
     * Only works for comparable types, and class type L = R = T.
     */
    public <T extends Comparable> NullablePair<T, T> sorted() {
        // assert L, R and T are the same class

        if (left == null) {
            return (NullablePair<T, T>)this;
        }

        if (right == null) {
            return new NullablePair<>(null, (T)this.left);
        }

        var l = (Comparable) left;
        var r = (Comparable) right;
        if (l.compareTo(r) < 0) {
            return (NullablePair<T, T>) this;
        }
        return new NullablePair<>((T)right, (T)left);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof NullablePair otherObj)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (left == null) {
            if (right == null) {
                return otherObj.left == null && otherObj.right == null;
            } else {
                return otherObj.left == null && right.equals(otherObj.right);
            }
        } else if (right == null) {
            return left.equals(otherObj.left) && otherObj.right == null;
        } else {
            return left.equals(otherObj.left) && right.equals(otherObj.right);
        }
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }

    @Override
    public int hashCode() {
        return "%s-%s".formatted(left, right).hashCode();
    }
}
