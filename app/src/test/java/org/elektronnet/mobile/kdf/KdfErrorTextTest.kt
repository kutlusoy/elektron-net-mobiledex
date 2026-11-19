package org.elektronnet.mobile.kdf

import org.junit.Assert.assertEquals
import org.junit.Test

class KdfErrorTextTest {

    @Test
    fun keepsTheLastActionableSentence() {
        val message = """KDF RPC error HTTP 500: {"error":"rpc:198] RPC call failed: lp_ordermatch:4657] lp_ordermatch:4786] Rel amount 0.00009 is too low, required: 0.0001"}"""
        assertEquals("Rel amount 0.00009 is too low, required: 0.0001", KdfErrorText.readable(message))
    }

    @Test
    fun worksWithoutTheHttpWrapper() {
        val message = """{"error":"rpc:198] RPC call failed: legacy:152] Deactivated coin"}"""
        assertEquals("Deactivated coin", KdfErrorText.readable(message))
    }

    @Test
    fun passesPlainTextThrough() {
        assertEquals("maker offline", KdfErrorText.readable("maker offline"))
    }

    @Test
    fun handlesNullAndEmpty() {
        assertEquals("", KdfErrorText.readable(null))
        assertEquals("", KdfErrorText.readable(""))
    }

    @Test
    fun keepsAnErrorWithoutMarkers() {
        val message = "KDF RPC error HTTP 500: something went wrong"
        assertEquals("something went wrong", KdfErrorText.readable(message))
    }
}
