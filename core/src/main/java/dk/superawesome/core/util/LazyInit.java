package dk.superawesome.core.util;

import java.util.function.Supplier;

public class LazyInit<T> {

    private final Supplier<T> get;
    private boolean called;
    private T val;

    public LazyInit<T> alreadyInitialized(T val) {
        return new LazyInit<>(() -> val);
    }

    private LazyInit(Supplier<T> get) {
        this.get = get;
    }

    public LazyInit() {
        this(null);
    }

    public T get() {
        if (!this.called && this.get != null) {
            this.val = this.get.get();
            this.called = true;
        }
        return this.val;
    }

    public T getOr(Supplier<T> get) {
        if (this.called) {
            return this.val;
        }

        this.called = true;
        this.val = get.get();
        return this.val;
    }
}
