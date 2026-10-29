package io.github.samolego.canta.core.adb

import io.github.samolego.canta.core.DeviceUser
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseUsersTest {

    @Test
    fun parsesUsers() {
        val output = "Users:\n\tUserInfo{0:samo_lego:4c13} running\n\tUserInfo{10:Delovni profil:1030} running\n"
        assertEquals(listOf(DeviceUser(0, "samo_lego"), DeviceUser(10, "Delovni profil")), parseUsers(output))
    }

    @Test
    fun namesMayContainColons() {
        assertEquals(listOf(DeviceUser(11, "Work: tools:v2")), parseUsers("\tUserInfo{11:Work: tools:v2:1030}"))
    }

    @Test
    fun skipsGarbageLines() {
        val output = "Users:\nnonsense\n\tUserInfo{x:Bad:1}\n\tUserInfo{5:NoFlags}\n\tUserInfo{7:Ok:10\n\n\tUserInfo{3:Fine:1} running"
        assertEquals(listOf(DeviceUser(3, "Fine")), parseUsers(output))
    }

    @Test
    fun emptyOutputHasNoUsers() {
        assertEquals(emptyList(), parseUsers(""))
    }
}
