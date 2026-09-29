package com.codedoc;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashTest {
    @Test
    public void testHash() {
        System.out.println("HASH: " + new BCryptPasswordEncoder().encode("demo1234"));
    }
}
