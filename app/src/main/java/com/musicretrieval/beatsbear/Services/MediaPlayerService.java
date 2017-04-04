package com.musicretrieval.beatsbear.Services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.musicretrieval.beatsbear.Activities.MainActivity;
import com.musicretrieval.beatsbear.Models.Song;
import com.musicretrieval.beatsbear.R;
import com.musicretrieval.beatsbear.Utils.PlaybackStatus;
import com.musicretrieval.beatsbear.Utils.StorageUtil;

import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;

public class MediaPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener{

    private AudioManager audioManager;
    public static AudioDispatcher dispatcher;
    private WaveformSimilarityBasedOverlapAdd wsola;

    private int songIndex = -1;
    private Song currentSong;
    private ArrayList<Song> songs;

    private double tempo = 1.0;
    private long currentTime;
    private double currentBpm;
    private boolean playing;

    private final int SAMPLE_RATE = 44100;

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    public static final String ACTION_PLAY = "com.musicretrieval.beatsbear.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.musicretrieval.beatsbear.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.musicretrieval.beatsbear.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.musicretrieval.beatsbear.ACTION_NEXT";
    public static final String ACTION_STOP = "com.musicretrieval.beatsbear.ACTION_STOP";
    public static final String SONG_CURRENT_TIME = "com.musicretrieval.beatsbear.SONG_CURRENT_TIME";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    public MediaPlayerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        // ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        // Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();
    }

    private void initMediaPlayer() {
        new AndroidFFMPEGLocator(this);

        tempo = 1.0;
        wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(tempo, SAMPLE_RATE));

        dispatcher = AudioDispatcherFactory.fromPipe(currentSong.getData(), SAMPLE_RATE , wsola.getInputBufferSize(), wsola.getOverlap());
        AndroidAudioPlayer audioPlayer = new AndroidAudioPlayer(dispatcher.getFormat(), wsola.getInputBufferSize(), AudioManager.STREAM_MUSIC);
        AudioProcessor processor = new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                if (playing) {
                    updateTimes();
                }
                return true;
            }

            @Override
            public void processingFinished() {
                if (playing) {
                    next();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING);
                }
            }
        };

        wsola.setDispatcher(dispatcher);
        dispatcher.addAudioProcessor(wsola);
        dispatcher.addAudioProcessor(audioPlayer);
        dispatcher.addAudioProcessor(processor);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resume();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pause();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                next();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                previous();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                switch (action) {
                    case "UPDATE_TEMPO":
                        updateTempo(extras.getDouble("TEMPO_AMOUNT"));
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void updateTimes() {
        currentTime = (long) dispatcher.secondsProcessed();
        Intent intent = new Intent(SONG_CURRENT_TIME);
        intent.putExtra("SONG_CURRENT_TIME", currentTime);
        sendBroadcast(intent);
    }

    public void updateTempo(double amount) {
        tempo = amount;
        currentBpm = (int) (currentSong.getFeatures().bpm * tempo);
        int SEQUENCE_MODEL = 82;
        int WINDOW_MODEL = 28;
        int OVERLAP_MODEL = 12;
        wsola.setParameters(new WaveformSimilarityBasedOverlapAdd.Parameters(tempo, SAMPLE_RATE, SEQUENCE_MODEL, WINDOW_MODEL, OVERLAP_MODEL));
    }

    public void next() {
        if (songIndex == songs.size() - 1) {
            //if last in playlist
            songIndex = 0;
            currentSong = songs.get(songIndex);
        } else {
            //get next in playlist
            currentSong = songs.get(++songIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(songIndex);
        if (playing && dispatcher != null) {
            stop();
            initMediaPlayer();
            play(0);
        } else {
            currentTime = 0;
        }
    }

    public void previous() {
        if (songIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            songIndex = songs.size() - 1;
            currentSong = songs.get(songIndex);
        } else {
            //get previous in playlist
            currentSong = songs.get(--songIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(songIndex);
        if (playing && dispatcher != null) {
            stop();
            initMediaPlayer();
            play(0);
        } else {
            currentTime = 0;
        }
    }

    public void seek(long seconds) {
        stop();
        play(seconds);
    }

    public void play(long seconds) {
        try {
            playing = true;
            tempo = currentBpm / currentSong.getFeatures().bpm;
            dispatcher = AudioDispatcherFactory.fromPipe(currentSong.getData(), SAMPLE_RATE , wsola.getInputBufferSize(), wsola.getOverlap());
            AndroidAudioPlayer audioPlayer = new AndroidAudioPlayer(dispatcher.getFormat(), wsola.getInputBufferSize(), AudioManager.STREAM_MUSIC);
            AudioProcessor processor = new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    if (playing) {
                        updateTimes();
                    }
                    return true;
                }

                @Override
                public void processingFinished() {
                    if (playing) {
                        next();
                        updateMetaData();
                        buildNotification(PlaybackStatus.PLAYING);
                    }
                }
            };

            wsola.setDispatcher(dispatcher);
            dispatcher.skip(seconds);
            dispatcher.addAudioProcessor(wsola);
            dispatcher.addAudioProcessor(audioPlayer);
            dispatcher.addAudioProcessor(processor);
            new Thread(dispatcher, "Audio Dispatcher").start();
        } catch (Exception ex) {
            ex.printStackTrace();
            stopSelf();
        }
    }

    public void stop() {
        if (dispatcher == null) return;
        playing = false;
        dispatcher.stop();
    }

    public void pause() {
        if (playing) {
            currentTime = (long) dispatcher.secondsProcessed();
            stop();
        }
    }

    public void resume() {
        if (!playing && dispatcher != null) {
            play(currentTime);
        }
    }

    private final IBinder iBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated.
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (dispatcher == null) initMediaPlayer();
                else if (!playing) play(0);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media dispatcherThread
                if (playing) stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media dispatcherThread because playback
                // is likely to resume
                if (playing) pause();
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            songs = storage.loadAudio();
            songIndex = storage.loadAudioIndex();

            if (songIndex != -1 && songIndex < songs.size()) {
                //index is in a valid range
                currentSong = songs.get(songIndex);
                currentBpm = currentSong.getFeatures().bpm;
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
                play(0);
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dispatcher != null) {
            stop();
        }

        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (dispatcher != null) {
                            pause();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (dispatcher != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resume();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pause();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            songIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();

            if (songIndex != -1 && songIndex < songs.size()) {
                //index is in a valid range
                currentSong = songs.get(songIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stop();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);

            resume();
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.blues); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle())
                .build());
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.blues); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(currentSong.getArtist())
                .setContentTitle(currentSong.getAlbum())
                .setContentInfo(currentSong.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }
}
