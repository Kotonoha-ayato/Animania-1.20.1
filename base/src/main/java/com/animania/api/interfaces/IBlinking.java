package com.animania.api.interfaces;

/** Stable client-state contract for animals whose renderer supports blinking. */
public interface IBlinking {
    int getBlinkTimer();

    void setBlinkTimer(int ticks);
}
