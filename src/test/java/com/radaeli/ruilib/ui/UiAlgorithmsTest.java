package com.radaeli.ruilib.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiAlgorithmsTest {
    @Test public void radialHitTestingMatchesEverySegmentCenter() {
        Bounds bounds = new Bounds(10, 20, 144, 144);
        var center = RadialGeometry.center(bounds);
        for (int count = 1; count <= 32; count++) {
            for (int segment = 0; segment < count; segment++) {
                double angle = RadialGeometry.segmentCenterAngle(segment, count);
                assertEquals(segment, RadialGeometry.segmentAt(center.x() + Math.cos(angle) * 50,
                        center.y() + Math.sin(angle) * 50, center, 15, 71, count));
            }
        }
        assertEquals(-1, RadialGeometry.segmentAt(center.x(), center.y(), center, 15, 71, 4));
        assertEquals(-1, RadialGeometry.segmentAt(1000, 1000, center, 15, 71, 4));
        assertEquals(-1, RadialGeometry.segmentAt(Double.NaN, 0, center, 15, 71, 4));
    }

    @Test public void easingRemainsMonotonicWithExactEndpoints() {
        for (UiEasing easing : UiEasing.values()) {
            assertEquals(0, easing.apply(0), 0);
            assertEquals(1, easing.apply(1), 0);
            double last = 0;
            for (int i = 1; i <= 1000; i++) {
                double value = easing.apply(i / 1000.0);
                assertTrue(value >= last && value <= 1);
                last = value;
            }
        }
        assertEquals(0.5, UiEasing.EASE_IN_OUT.apply(0.5), 0);
        assertEquals(140, UiMotion.DEFAULT.transitionDurationMillis());
    }

    @Test public void colorsPreserveAlphaAndRejectMalformedValues() {
        assertEquals(0xFF123456, UiColor.hex("#123456").argb());
        assertEquals(0x80123456, UiColor.hex("#80123456").argb());
        assertEquals(UiColor.GRAY, UiColor.named(" Grey "));
        assertThrows(IllegalArgumentException.class, () -> UiColor.hex("#bad"));
        assertThrows(IllegalArgumentException.class, () -> UiColor.hex("#GG1234"));
        assertThrows(IllegalArgumentException.class, () -> new UiMotion(-1, UiEasing.LINEAR));
    }

    @Test public void stepperRangesHoldTheirBoundsAndOptionsStartFromNothing() {
        assertEquals(5, Stepper.stepRange(4, 1, 0, 32));
        assertEquals(32, Stepper.stepRange(30, 10, 0, 32));
        assertEquals(0, Stepper.stepRange(3, -10, 0, 32));
        assertEquals(Integer.MAX_VALUE, Stepper.stepRange(Integer.MAX_VALUE - 1, 10, 0, Integer.MAX_VALUE));
        assertEquals(0, Stepper.stepIndex(-1, 1, 4, true));
        assertEquals(3, Stepper.stepIndex(-1, -1, 4, true));
        assertEquals(0, Stepper.stepIndex(3, 1, 4, true));
        assertEquals(3, Stepper.stepIndex(3, 1, 4, false));
        assertEquals(0, Stepper.stepIndex(0, -1, 4, false));
    }
}
