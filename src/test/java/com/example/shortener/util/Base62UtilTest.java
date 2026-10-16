package com.example.shortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base62UtilTest {

    @Test
    @DisplayName("Encode and decode round-trip")
    void testEncodeDecode() {
        long original = 123456789L;
        String encoded = Base62Util.encode(original);
        long decoded = Base62Util.decode(encoded);
        assertEquals(original, decoded, "Decoded value should match original");
    }

    @Test
    @DisplayName("Encode zero")
    void testEncodeZero() {
        assertEquals("0", Base62Util.encode(0L), "Zero should encode to \"0\"");
    }

    @Test
    @DisplayName("Decode invalid characters throws")
    void testDecodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Base62Util.decode("!*@"));
    }
}
