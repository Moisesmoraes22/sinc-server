package com.apksinc.monitor

import com.apksinc.monitor.domain.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerStatusTest {

    @Test
    fun `parses known raw statuses`() {
        assertEquals(ServerStatus.ONLINE, ServerStatus.fromRaw("ONLINE"))
        assertEquals(ServerStatus.ONLINE, ServerStatus.fromRaw("online"))
        assertEquals(ServerStatus.OFFLINE, ServerStatus.fromRaw("OFFLINE"))
        assertEquals(ServerStatus.ATENCAO, ServerStatus.fromRaw("ATENCAO"))
    }

    @Test
    fun `unknown status falls back to attention`() {
        assertEquals(ServerStatus.ATENCAO, ServerStatus.fromRaw("desconhecido"))
    }
}
