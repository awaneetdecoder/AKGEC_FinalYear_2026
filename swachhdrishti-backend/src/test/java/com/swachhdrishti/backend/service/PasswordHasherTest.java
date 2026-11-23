package com.swachhdrishti.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void correctPasswordMatchesAndWrongOneDoesNot() {
        String stored = PasswordHasher.hash("Str0ng-pass-2026");
        assertTrue(PasswordHasher.matches("Str0ng-pass-2026", stored));
        assertFalse(PasswordHasher.matches("str0ng-pass-2026", stored));
        assertFalse(PasswordHasher.matches("", stored));
    }

    @Test
    void sameSaltedPasswordGivesDifferentHashesAndNeverStoresPlaintext() {
        String a = PasswordHasher.hash("same-password");
        String b = PasswordHasher.hash("same-password");
        assertNotEquals(a, b);
        assertFalse(a.contains("same-password"));
        assertEquals(4, a.split("\\$").length);
        assertTrue(PasswordHasher.matches("same-password", a));
        assertTrue(PasswordHasher.matches("same-password", b));
    }

    @Test
    void garbageOrMissingHashesNeverMatch() {
        assertFalse(PasswordHasher.matches("x", null));
        assertFalse(PasswordHasher.matches("x", ""));
        assertFalse(PasswordHasher.matches("x", "plain-text"));
        assertFalse(PasswordHasher.matches("x", "pbkdf2_sha256$notanumber$AAAA$BBBB"));
        assertFalse(PasswordHasher.matches(null, PasswordHasher.hash("x")));
    }
}
