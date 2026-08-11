package com.animania.api;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Optional status bridge for addon block entities.  Base's Jade and TOP
 * integrations can consume this without depending on any addon classes.
 */
public interface IAnimaniaProbeBlock {
    List<Component> getAnimaniaProbeInfo();
}
