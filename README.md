# TrackMix

A realtime tempo adjustment application for runners.

[![Build Status](https://travis-ci.org/musicretreival/TrackMix.svg?branch=master)](https://travis-ci.org/musicretreival/TrackMix)

# Build Guide

To install, this Android project requires the following library:
https://github.com/WritingMinds/ffmpeg-android/releases
Download `prebuilt-binaries.zip`, you will get three files:
- armeabi-v7a/ffmpeg
- armeabi-v7a-neon/ffmpeg
- x86/ffmpeg
Rename each of the `ffmpeg` to `<FOLDER_NAME>_ffmpeg` and place it in `app/build/intermediates/assets`. Your `app/build/intermediates/assets` folder should have: `armeabi-v7a_ffmpeg`, `armeabi-v7a-neon_ffmpeg`, and `x86_ffmpeg`

If you are using Android emulator, you will want to put music in the emulator:
- download some songs
- use Android Device Monitor
- store in /storage/sdcard/Music, or a similar path

# Technologies
[TarsosDSP](https://github.com/JorenSix/TarsosDSP) for audio processing
