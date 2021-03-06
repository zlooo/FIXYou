package io.github.zlooo.fixyou.commons.pool;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = "-XX:-RestrictContended")
@Threads(Threads.MAX)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ObjectPoolPerformanceTest {

    private static final int POOL_SIZE = 10_000_000;
    private DefaultObjectPool<TestPoolableObject> defaultObjectPool;
    private ArrayBackedObjectPool<TestPoolableObject> arrayBackedObjectPool;


    @Setup
    public void setUp() {
        defaultObjectPool = new DefaultObjectPool<>(POOL_SIZE, TestPoolableObject::new, TestPoolableObject.class, POOL_SIZE);
        arrayBackedObjectPool = new ArrayBackedObjectPool<>(POOL_SIZE, TestPoolableObject::new, TestPoolableObject.class, POOL_SIZE);
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SampleTime})
    public TestPoolableObject defaultObjectPoolTest() {
        final TestPoolableObject poolableObject = defaultObjectPool.getAndRetain();
        poolableObject.release(); //TODO this is supposed to be get test not get and release test, move release out of mesurement
        return poolableObject;
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SampleTime})
    public TestPoolableObject noThreadLocalObjectPoolTest() {
        final TestPoolableObject poolableObject = arrayBackedObjectPool.getAndRetain();
        poolableObject.release(); //TODO this is supposed to be get test not get and release test, move release out of mesurement
        return poolableObject;
    }
}
