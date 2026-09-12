package com.example

import com.example.data.model.UserRole
import com.example.data.model.VirtualMachine
import com.example.data.model.VmActionType
import com.example.data.model.VmPowerState
import org.junit.Assert.*
import org.junit.Test

class RbacActionVisibilityTest {

    @Test
    fun `viewer role has no vm action permissions`() {
        val viewer = UserRole.fromString("viewer")
        assertEquals(UserRole.VIEWER, viewer)
        assertFalse(viewer.canPerformVmActions)
        assertFalse(viewer.canManageAlerts)
    }

    @Test
    fun `operator role has vm action permissions`() {
        val operator = UserRole.fromString("operator")
        assertEquals(UserRole.OPERATOR, operator)
        assertTrue(operator.canPerformVmActions)
        assertTrue(operator.canManageAlerts)
    }

    @Test
    fun `admin role has vm action permissions`() {
        val admin = UserRole.fromString("admin")
        assertEquals(UserRole.ADMIN, admin)
        assertTrue(admin.canPerformVmActions)
        assertTrue(admin.canManageAlerts)
    }

    @Test
    fun `vm state determines available actions`() {
        val runningVm = VirtualMachine(
            id = "vm-1",
            name = "Test Running VM",
            powerState = "RUNNING"
        )
        val runningActions = runningVm.availableActions()
        assertTrue(runningActions.contains(VmActionType.POWER_OFF))
        assertTrue(runningActions.contains(VmActionType.RESTART))
        assertTrue(runningActions.contains(VmActionType.RESET))
        assertTrue(runningActions.contains(VmActionType.SUSPEND))
        assertFalse(runningActions.contains(VmActionType.POWER_ON))

        val stoppedVm = VirtualMachine(
            id = "vm-2",
            name = "Test Stopped VM",
            powerState = "STOPPED"
        )
        val stoppedActions = stoppedVm.availableActions()
        assertTrue(stoppedActions.contains(VmActionType.POWER_ON))
        assertFalse(stoppedActions.contains(VmActionType.POWER_OFF))
    }

    @Test
    fun `destructive vm actions require confirmation warning`() {
        assertTrue(VmActionType.POWER_OFF.isDestructive)
        assertTrue(VmActionType.RESTART.isDestructive)
        assertTrue(VmActionType.RESET.isDestructive)
        assertTrue(VmActionType.SUSPEND.isDestructive)
        assertFalse(VmActionType.POWER_ON.isDestructive)
    }
}
