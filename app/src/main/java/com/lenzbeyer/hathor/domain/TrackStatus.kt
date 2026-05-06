package com.lenzbeyer.hathor.domain

enum class TrackStatus {
    Pending, Resolving, Downloading, Transcoding, Tagging, Done, Skipped, Failed;

    val isTerminal: Boolean get() = this == Done || this == Skipped || this == Failed
    val isActive:   Boolean get() = this == Resolving || this == Downloading ||
                                    this == Transcoding || this == Tagging
}
