package com.example.shortener.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class Base62Test {

    @Test
    void encodesKnownValues() {
        assertEquals("0", Base62.encode(0));
        assertEquals("z", Base62.encode(35));
        assertEquals("A", Base62.encode(36));
        assertEquals("10", Base62.encode(62));     // first two-character code
    }

    @Test
    void differentNumbersGiveDifferentCodes() {
        assertNotEquals(Base62.encode(100_000_001L), Base62.encode(100_000_002L));
    }
}