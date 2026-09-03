class Counter {
    private final Object lockA = new Object();
    private final Object lockB = new Object();
    private int count;

    void increment() {
        synchronized (lockA) {
            count++;
        }
    }

    void reset() {
        synchronized (lockB) {
            count = 0;
        }
    }
}
