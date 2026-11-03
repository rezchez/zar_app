package com.zarfolio.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlValidatorTest {
    @Test fun addsHttpsAndNormalizesTrailingSlash() { assertEquals("https://zar.example", ServerUrlValidator.normalize("zar.example/")) }
    @Test fun acceptsPrivateLanHttp() { assertEquals("http://192.168.1.20:8090", ServerUrlValidator.normalize("http://192.168.1.20:8090/")) }
    @Test fun rejectsPublicHttpAndCredentials() {
        assertNull(ServerUrlValidator.normalize("http://zar.example"))
        assertNull(ServerUrlValidator.normalize("https://user:pass@zar.example"))
    }
}
