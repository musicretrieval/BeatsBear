# BeatsBear

<img src="beatsbear.png" alt="beatsbear" width="250" height="250">

A realtime tempo adjustment application with genre classification.

[![Build Status](https://travis-ci.org/musicretrieval/BeatsBear.svg?branch=master)](https://travis-ci.org/musicretrieval/BeatsBear)

# Process
The application first loads all the music that it can find on the user's device. From there TarsosDSP was used to estimate the BPM and MFCC's of each song. Using the MFCC's as a single vector feature, I was able to train an SVM classifier with Weka. After all the songs have been read and their features applied, the application will classify the songs and apply a tag. 

The BPM and genre are used as a recommendation filter, which is still under work. Currently, if the songs BPM < 130 and the song was classified as Blues, Classical, or Country, then it is considered as relaxing music. Otherwise, it is considered activiating music. 

From there, you can play music and play around with the song's tempo real time.

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
[Weka](http://www.cs.waikato.ac.nz/ml/weka/) for machine learning

# References
Genre icons made by [Freepik](http://www.freepik.com) from [flaticon](http://www.flaticon.com) is licensed by [Creative Commons BY 3.0](http://creativecommons.org/licenses/by/3.0/)
