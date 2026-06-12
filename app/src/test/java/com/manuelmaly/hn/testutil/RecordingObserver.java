package com.manuelmaly.hn.testutil;

import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

/** Observer that records every emitted value, for ordering assertions. */
public class RecordingObserver<T> implements Observer<T> {

    public final List<T> values = new ArrayList<T>();

    @Override
    public void onChanged(T value) {
        values.add(value);
    }

    public T last() {
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    public int size() {
        return values.size();
    }
}
