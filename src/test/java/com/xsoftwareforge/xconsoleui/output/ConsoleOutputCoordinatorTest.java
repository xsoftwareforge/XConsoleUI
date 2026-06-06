package com.xsoftwareforge.xconsoleui.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ConsoleOutputCoordinatorTest {

    @Test
    public void publishesImmediatelyWhenIdle() {
        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        coordinator.publish("hello");

        assertEquals(List.of("hello"), sink);
    }

    @Test
    public void buffersDuringMenuAndFlushesOnLeave() {
        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        coordinator.enterInteraction(SessionState.MENU);
        coordinator.publish("buffered-1");
        coordinator.publish("buffered-2");
        assertTrue("output must be held while a menu is active", sink.isEmpty());

        coordinator.leaveInteraction(SessionState.IDLE);
        assertEquals(List.of("buffered-1", "buffered-2"), sink);
    }

    @Test
    public void nestedInteractionFlushesOnlyAtOutermostLeave() {
        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        coordinator.enterInteraction(SessionState.MENU);
        coordinator.enterInteraction(SessionState.WIZARD);
        coordinator.publish("inner");

        coordinator.leaveInteraction(SessionState.IDLE); // back to MENU, still buffering
        assertTrue(sink.isEmpty());

        coordinator.leaveInteraction(SessionState.IDLE); // back to IDLE, flush
        assertEquals(List.of("inner"), sink);
        assertEquals(SessionState.IDLE, coordinator.getState());
    }

    @Test
    public void trimsTrailingLineBreaks() {
        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        coordinator.publish("line\r\n");

        assertEquals(List.of("line"), sink);
    }

    @Test
    public void detachFlushesBufferedOutput() {
        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        coordinator.enterInteraction(SessionState.MENU);
        coordinator.publish("pending");
        coordinator.detachPrinter();

        assertEquals(List.of("pending"), sink);
        assertEquals(SessionState.DISABLED, coordinator.getState());
    }

    @Test
    public void recordsHistoryEvenWhileBuffering() {
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        coordinator.enterInteraction(SessionState.MENU);
        coordinator.publish("a");
        coordinator.publish("b");
        coordinator.leaveInteraction(SessionState.IDLE);
        coordinator.publish("c");

        assertEquals(List.of("a", "b", "c"), coordinator.history());

        coordinator.clearHistory();
        assertTrue(coordinator.history().isEmpty());
    }

    @Test
    public void historySplitsMultiLineMessages() {
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        coordinator.publish("line1\nline2\n");

        assertEquals(List.of("line1", "line2"), coordinator.history());
    }

    @Test
    public void ignoresNullAndEmptyMessages() {
        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        coordinator.publish(null);
        coordinator.publish("");

        assertTrue(sink.isEmpty());
    }
}
