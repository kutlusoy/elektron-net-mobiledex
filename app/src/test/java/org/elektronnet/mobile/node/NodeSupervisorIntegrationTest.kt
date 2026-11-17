package org.elektronnet.mobile.node

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class NodeSupervisorIntegrationTest {
    @Test
    fun startsRegtestNodeQueriesItAndStopsGracefully() {
        val binary = System.getenv("ELEKTROND_BIN")
        assumeTrue("ELEKTROND_BIN not set", binary != null && File(binary).canExecute())
        val dir = Files.createTempDirectory("node-it").toFile()
        // free ports: a running devnet forwards 28332/28333 to a phone, so fixed
        // numbers make this test fail with "Unable to bind any endpoint for RPC server"
        fun freePort(): Int = java.net.ServerSocket(0).use { it.localPort }
        val config = NodeConfig(File(binary!!), dir, Chain.Regtest, rpcPort = freePort(), p2pPort = freePort(), listen = false)
        val node = NodeSupervisor(config, pollIntervalMs = 100)
        node.start()
        val end = System.currentTimeMillis() + 60_000
        while (node.state == NodeState.Starting && System.currentTimeMillis() < end) Thread.sleep(100)
        assertEquals(NodeState.Running, node.state)
        val status = node.status()
        assertEquals("regtest", status.chain)
        val schedule = node.telemetry().schedule
        assertTrue(schedule != null)
        assertEquals(0L, schedule!!.height)
        assertTrue(status.pruned)
        assertTrue(node.stop())
        val stopEnd = System.currentTimeMillis() + 10_000
        while (node.state != NodeState.Stopped && System.currentTimeMillis() < stopEnd) Thread.sleep(50)
        assertEquals(NodeState.Stopped, node.state)
        assertTrue(File(dir, "regtest").isDirectory)
    }
}
