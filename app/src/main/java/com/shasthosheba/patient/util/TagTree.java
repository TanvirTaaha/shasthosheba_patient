package com.shasthosheba.patient.util;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * most of the codes other than getTag() are copied from {@link Timber.DebugTree}
 * @author TanvirTaaha
 * implementation 'com.jakewharton.timber:timber:4.7.1'
 */
public class TagTree extends Timber.DebugTree {
    protected final String tagPrefix;
    protected final boolean prefixTag;

    public TagTree(String tagPrefix, boolean prefixTag) {
        super();
        this.tagPrefix = tagPrefix;
        this.prefixTag = prefixTag;
    }

    @Override
    protected void log(int priority, String tag, @NotNull String message, Throwable t) {
        if (prefixTag) {
            super.log(priority, tagPrefix + "-" + tag, message, t);
        } else {
            super.log(priority, tagPrefix, message, t);
        }
    }
}
