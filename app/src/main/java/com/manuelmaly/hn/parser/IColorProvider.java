package com.manuelmaly.hn.parser;

/**
 * Abstraction over {@code android.graphics.Color} so the comments parser can run
 * on a plain JVM in unit tests. Returns an ARGB int, same contract as
 * {@code Color.rgb(r, g, b)}.
 */
public interface IColorProvider {

    int rgb(int r, int g, int b);

}
