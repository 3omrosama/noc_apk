package com.example

import com.example.data.security.UrlNormalizer
import org.junit.Assert.*
import org.junit.Test

class UrlNormalizationTest {

    @Test
    fun `test public domain normalization`() {
        assertEquals("https://noc.example.com", UrlNormalizer.normalize("https://noc.example.com"))
        assertEquals("https://noc.example.com", UrlNormalizer.normalize("https://noc.example.com/"))
        assertEquals("https://noc.example.com", UrlNormalizer.normalize("https://noc.example.com///"))
        assertEquals("https://noc.example.com", UrlNormalizer.normalize("  noc.example.com  "))
    }

    @Test
    fun `test private subdomain with custom path`() {
        assertEquals("https://noc.it.nds-technology.com", UrlNormalizer.normalize("https://noc.it.nds-technology.com"))
        assertEquals("https://noc.it.nds-technology.com", UrlNormalizer.normalize("noc.it.nds-technology.com"))
    }

    @Test
    fun `test IP address and custom port normalization`() {
        assertEquals("http://172.16.0.12:3000", UrlNormalizer.normalize("http://172.16.0.12:3000"))
        assertEquals("http://172.16.0.12:3000", UrlNormalizer.normalize("172.16.0.12:3000"))
        assertEquals("http://192.168.1.50:3000", UrlNormalizer.normalize("192.168.1.50:3000"))
        assertEquals("http://10.0.0.1", UrlNormalizer.normalize("10.0.0.1"))
    }

    @Test
    fun `test LAN hostname with port`() {
        assertEquals("http://noc-server:8080", UrlNormalizer.normalize("http://noc-server:8080"))
        assertEquals("http://noc-server:8080", UrlNormalizer.normalize("noc-server:8080"))
    }

    @Test
    fun `test retro fit base url generation`() {
        assertEquals("https://noc.example.com/", UrlNormalizer.toRetrofitBaseUrl("https://noc.example.com"))
        assertEquals("http://172.16.0.12:3000/", UrlNormalizer.toRetrofitBaseUrl("http://172.16.0.12:3000/"))
    }

    @Test
    fun `test websocket url conversion`() {
        assertEquals("wss://noc.example.com/ws", UrlNormalizer.toWebSocketUrl("https://noc.example.com"))
        assertEquals("ws://172.16.0.12:3000/ws", UrlNormalizer.toWebSocketUrl("http://172.16.0.12:3000"))
    }

    @Test
    fun `test url validity check`() {
        assertTrue(UrlNormalizer.isValidUrl("https://noc.example.com"))
        assertTrue(UrlNormalizer.isValidUrl("http://172.16.0.12:3000"))
        assertTrue(UrlNormalizer.isValidUrl("192.168.1.50:3000"))
        assertFalse(UrlNormalizer.isValidUrl(""))
        assertFalse(UrlNormalizer.isValidUrl("   "))
    }
}
