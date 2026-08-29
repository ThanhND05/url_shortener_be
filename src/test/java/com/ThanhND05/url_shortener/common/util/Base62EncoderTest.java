package com.ThanhND05.url_shortener.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void testEncode_ValidNumbers() {
        assertEquals("0", Base62Encoder.encode(0));
        assertEquals("1", Base62Encoder.encode(1));
        assertEquals("A", Base62Encoder.encode(10));
        assertEquals("a", Base62Encoder.encode(36));
        assertEquals("1C", Base62Encoder.encode(74));
        assertEquals("Q0u", Base62Encoder.encode(100000));
        assertEquals("4C91", Base62Encoder.encode(999999));
    }

    @Test
    void testEncode_NegativeNumber_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
    }

    @Test
    void testDecode_ValidStrings() {
        assertEquals(0, Base62Encoder.decode("0"));
        assertEquals(1, Base62Encoder.decode("1"));
        assertEquals(10, Base62Encoder.decode("A"));
        assertEquals(36, Base62Encoder.decode("a"));
        assertEquals(74, Base62Encoder.decode("1C"));
        assertEquals(100000, Base62Encoder.decode("Q0u"));
        assertEquals(999999, Base62Encoder.decode("4C91"));
    }

    @Test
    void testDecode_InvalidCharacters_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("q0-U"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(" "));
    }

    @Test
    void testDecode_NullOrEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
    }
}
