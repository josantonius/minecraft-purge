package dev.josantonius.minecraft.purge

import java.time.Duration
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player

class PurgeBossBar(
        private val title: String,
        private val color: BarColor,
        private val style: BarStyle
) {
    private var bossBar: BossBar
    private var elapsedTime: Duration = Duration.ZERO

    init {
        bossBar = Bukkit.createBossBar("", color, style)
        bossBar.let { it.setProgress(0.0) }
        bossBar.let { it.setVisible(false) }
    }

    fun show() {
        for (player in Bukkit.getOnlinePlayers()) {
            addPlayer(player)
        }
        bossBar.let { it.setVisible(true) }
    }

    fun update(totalDuration: Duration) {
        elapsedTime = elapsedTime.plusSeconds(1)
        bossBar.let {
            val progress = elapsedTime.toMillis().toDouble() / totalDuration.toMillis()
            it.setProgress(progress.coerceIn(0.0, 1.0))

            val remainingTime = totalDuration.minus(elapsedTime)
            val daysRemaining = remainingTime.toDays()
            val hoursRemaining = remainingTime.minusDays(daysRemaining).toHours()
            val minutesRemaining =
                    remainingTime.minusDays(daysRemaining).minusHours(hoursRemaining).toMinutes()
            val secondsRemaining =
                    remainingTime
                            .minusDays(daysRemaining)
                            .minusHours(hoursRemaining)
                            .minusMinutes(minutesRemaining)
                            .seconds

            val timeFormat =
                    when {
                        daysRemaining > 0 ->
                                "%dd:%02dh:%02dm:%02ds".format(
                                        daysRemaining,
                                        hoursRemaining,
                                        minutesRemaining,
                                        secondsRemaining
                                )
                        hoursRemaining > 0 ->
                                "%02dh:%02dm:%02ds".format(
                                        hoursRemaining,
                                        minutesRemaining,
                                        secondsRemaining
                                )
                        minutesRemaining > 0 ->
                                "%02dm:%02ds".format(minutesRemaining, secondsRemaining)
                        else -> "%02ds".format(secondsRemaining)
                    }

            it.setTitle(title + " $timeFormat")
        }
    }

    fun hidden() {
        bossBar.let {
            it.setProgress(0.0)
            it.setVisible(false)
            it.removeAll()
        }
        elapsedTime = Duration.ZERO
    }

    fun getElapsedTime(): Duration {
        return elapsedTime
    }

    fun isVisible(): Boolean {
        return bossBar.isVisible()
    }

    fun addPlayer(player: Player) {
        bossBar.addPlayer(player)
    }

    fun removePlayer(player: Player) {
        bossBar.removePlayer(player)
    }
}
