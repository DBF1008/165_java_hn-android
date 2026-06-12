package com.manuelmaly.hn.parser;

import android.graphics.Color;

import javax.inject.Inject;

/**
 * Default {@link IColorProvider}, delegating to {@code android.graphics.Color}.
 */
public class AndroidColorProvider implements IColorProvider {

    @Inject
    public AndroidColorProvider() {
    }

    @Override
    public int rgb(int r, int g, int b) {
        return Color.rgb(r, g, b);
    }

}
