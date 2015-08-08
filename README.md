# PauseResumeAudioRecorder

This library provides a .WAV recorder that can be paused and resumed. The current Android media recorder does not support pause/resume functionality (http://developer.android.com/reference/android/media/MediaRecorder.html),
so this was created.

## How to Pull into your Project

Put  "compile 'com.github.republicofgavin:pauseresumeaudiorecorder:1.0'" in your projects dependency section of your build.gradle.
## Licensing
This project uses the Apache License.

## Tech Design
The recorder uses the AudioRecord Android object to do its recording. Once recording begins, a thread is created which dumps
the audio data into a temporary PCM file. If the recorder is paused during this workflow, the thread just sleeps until resume/stop are entered.
Once the user calls stop, the thread converts the PCM file into the specified WAV file. If an error occurs during this process, the state of the
media recorder is set to a error state(defined in the media recorder file). Once the media recorder has been stopped, it can't be reused. Thus, you should create a new instance and use that one.
