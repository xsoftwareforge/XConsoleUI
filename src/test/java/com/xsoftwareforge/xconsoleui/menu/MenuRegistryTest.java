package com.xsoftwareforge.xconsoleui.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class MenuRegistryTest {

    @Test
    public void preservesRegistrationOrder() {
        MenuRegistry registry = new MenuRegistry();
        Menu a = new StubMenu("a", "A");
        Menu b = new StubMenu("b", "B");
        Menu c = new StubMenu("c", "C");

        registry.addMenu(a);
        registry.addMenu(b);
        registry.addMenu(c);

        assertEquals(List.of(a, b, c), registry.menus());
        assertEquals(3, registry.size());
        assertFalse(registry.isEmpty());
    }

    @Test
    public void replacingSameIdKeepsPositionAndReturnsPrevious() {
        MenuRegistry registry = new MenuRegistry();
        Menu first = new StubMenu("dup", "First");
        Menu second = new StubMenu("dup", "Second");
        registry.addMenu(new StubMenu("before", "Before"));
        registry.addMenu(first);
        registry.addMenu(new StubMenu("after", "After"));

        Menu previous = registry.addMenu(second);

        assertSame(first, previous);
        List<Menu> menus = registry.menus();
        assertEquals(3, menus.size());
        assertSame(second, menus.get(1));
    }

    @Test
    public void removeReturnsMenuAndUpdatesState() {
        MenuRegistry registry = new MenuRegistry();
        Menu a = new StubMenu("a", "A");
        registry.addMenu(a);

        assertTrue(registry.contains("a"));
        assertSame(a, registry.removeMenu("a"));
        assertFalse(registry.contains("a"));
        assertTrue(registry.isEmpty());
        assertNull(registry.removeMenu("a"));
    }

    @Test
    public void snapshotIsIndependentOfRegistry() {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new StubMenu("a", "A"));

        List<Menu> snapshot = registry.menus();
        registry.addMenu(new StubMenu("b", "B"));

        assertEquals(1, snapshot.size());
        assertEquals(2, registry.size());
    }

    @Test
    public void concurrentAddsAreAllVisibleAndConsistent() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        int threads = 8;
        int perThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int base = t * perThread;
            pool.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        String id = "m-" + (base + i);
                        registry.addMenu(new StubMenu(id, id));
                        // Concurrent reads must never throw (e.g. ConcurrentModification).
                        registry.menus();
                        registry.size();
                    }
                } catch (Throwable ex) {
                    failure.compareAndSet(null, ex);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue("threads did not finish in time", done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertNull("a worker thread failed", failure.get());
        assertEquals(threads * perThread, registry.size());
        assertEquals(threads * perThread, registry.menus().size());
    }

    @Test
    public void concurrentAddAndRemoveLeaveRegistryConsistent() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        int rounds = 500;
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Runnable adder = () -> {
            try {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    registry.addMenu(new StubMenu("shared-" + i, "S"));
                    registry.addHotkeyOption(HotkeyOption.of("opt-" + i, "O", ctx -> {
                    }));
                }
            } catch (Throwable ex) {
                failure.compareAndSet(null, ex);
            } finally {
                done.countDown();
            }
        };
        Runnable remover = () -> {
            try {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    registry.removeMenu("shared-" + i);
                    registry.hotkeyOptions();
                }
            } catch (Throwable ex) {
                failure.compareAndSet(null, ex);
            } finally {
                done.countDown();
            }
        };

        pool.execute(adder);
        pool.execute(remover);
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertNull(failure.get());
        // Every option was added and never removed, so all must be present.
        assertEquals(rounds, registry.hotkeyOptions().size());
    }

    private static final class StubMenu implements Menu {
        private final String id;
        private final String title;

        StubMenu(String id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of();
        }
    }
}
