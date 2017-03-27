# TempoBear 

![alt text](tempobear.png)

A realtime tempo adjustment application for runners.

[![Build Status](https://travis-ci.org/musicretrieval/TempoBear.svg?branch=master)](https://travis-ci.org/musicretrieval/TempoBear)

# Build Guide

Try building and running the application, if it doesn't work, the ffmpeg library might be missing.

To run, this Android project requires the following library:
https://github.com/WritingMinds/ffmpeg-android/releases
Download `prebuilt-binaries.zip`, you will get three files:
- armeabi-v7a/ffmpeg
- armeabi-v7a-neon/ffmpeg
- x86/ffmpeg

Rename each of the `ffmpeg` to `<FOLDER_NAME>_ffmpeg` and place it in `app/build/intermediates/assets`. Your `app/build/intermediates/assets` folder should have: `armeabi-v7a_ffmpeg`, `armeabi-v7a-neon_ffmpeg`, and `x86_ffmpeg`. If that does not work, try putting it in `app/build/intermediates/assets/debug`.

If you are using Android emulator, you will want to put music in the emulator:
- Download some songs
- Use Android Device Monitor
- /storage/sdcard/Music, or a similar path
- For API 23 it should be storage/emulated/0/Music
- May need to restart emulator

Enable storage permissions: 
- Settings -> Apps -> TrackMix -> Permissions -> Storage

# Troubleshooting

- You may need to use API 23, it seems that API 23+ does not work well with this project.

# Technologies
[TarsosDSP](https://github.com/JorenSix/TarsosDSP) for audio processing
