package io.github.yeeuou.theFinals.commands

import net.kyori.adventure.text.Component

abstract class AbstractCommand {
    protected fun text(str: String) = Component.text(str)
}
