package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.AnimateEntityPacket
import com.nukkitx.protocol.bedrock.packet.AnimatePacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleAntiBot.isBot
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.utils.timing.ClickTimer
import kotlin.math.pow

class ModuleKillAura : CheatModule("KillAura","自动攻击") {

    private val cpsValue = intValue("CPS", 7, 1, 20)
    private val rangeValue = floatValue("Range", 3.7f, 2f, 7f)
    private val attackModeValue = listValue("AttackMode", arrayOf("Single", "Multi"), "Single")
    private val rotationModeValue = listValue("RotationMode", arrayOf("Lock", "None","Slient"), "Lock")
    private val swingValue = listValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Both")
    private val swingSoundValue = boolValue("SwingSound", false)
    private val failRateValue = floatValue("FailRate", 0f, 0f, 1f)
    private val failSoundValue = boolValue("FailSound", true)
    private val clickTimer = ClickTimer()
    private var rotations: Pair<Float, Float>? = null
    override fun onEnable() {
        super.onEnable()
        chat("| KillAura Enabled !")
    }

    override fun onDisable() {
        super.onDisable()
        chat("| KillAura Disabled !")
    }
    @Listen
    fun onTick(event: EventTick) {
        if (cpsValue.get() < 20 && !clickTimer.canClick())
            return

        val session = event.session

        val range = rangeValue.get().pow(2)
        val entityList = session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < range && !it.isBot(session) }
        if (entityList.isEmpty()) return

        val swingMode = session.thePlayer.getSwingMode(swingValue.get())
        val aimTarget = if (Math.random() <= failRateValue.get()) {
            session.thePlayer.swing(swingMode, failSoundValue.get())
            entityList.first()
        } else {
            when(attackModeValue.get()) {
                "Multi" -> {
                    entityList.forEach {
                        session.thePlayer.attackEntity(it, swingMode, swingSoundValue.get())
                     }
                    entityList.first()
                }
                else -> (entityList.minByOrNull { it.distanceSq(event.session.thePlayer) } ?: return).also {
                    session.thePlayer.attackEntity(it, swingMode, swingSoundValue.get())
                }
            }
        }

        if (rotationModeValue.get() == "Lock") {
            session.thePlayer.silentRotation = toRotation(session.thePlayer.vec3Position, aimTarget.vec3Position)
        }
        if (rotationModeValue.get() == "Slient") {
            rotations = toRotation(session.thePlayer.vec3Position, aimTarget.vec3Position.add(0f, 1f, 0f)).let {
                (it.first - session.thePlayer.rotationYaw) * 0.8f + session.thePlayer.rotationYaw to it.second
            }
        }
        clickTimer.update(cpsValue.get(), cpsValue.get() + 1)
    }
}