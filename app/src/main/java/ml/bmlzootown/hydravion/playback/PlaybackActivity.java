package ml.bmlzootown.hydravion.playback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.source.hls.DefaultHlsExtractorFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.HashMap;

import kotlin.Unit;
import ml.bmlzootown.hydravion.R;
import ml.bmlzootown.hydravion.browse.MainFragment;
import ml.bmlzootown.hydravion.client.HydravionClient;
import ml.bmlzootown.hydravion.detail.DetailsActivity;
import ml.bmlzootown.hydravion.models.Video;

public class PlaybackActivity extends FragmentActivity {

    private HydravionClient client;

    private PlayerView playerView;
    private ImageView like;
    private ImageView dislike;
    private ImageView menu;
    private ImageView speed;
    private LinearLayout exo_playback_menu;
    private LinearLayout exo_settings_menu;
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaController;

    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private boolean resumed = false;

    private String url = "";
    private Video video;

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = HydravionClient.Companion.getInstance(this, getPreferences(Context.MODE_PRIVATE));
        setContentView(R.layout.activity_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Video video = (Video) getIntent().getSerializableExtra(DetailsActivity.Video);
        this.video = video;
        url = video.getVidUrl();

        playerView = findViewById(R.id.exoplayer);
        ((TextView) findViewById(R.id.exo_title)).setText(video.getTitle());
        like = findViewById(R.id.exo_like);
        dislike = findViewById(R.id.exo_dislike);
        menu = findViewById(R.id.exo_menu);
        exo_playback_menu = findViewById(R.id.exo_playback_menu);
        exo_settings_menu = findViewById(R.id.exo_settings_menu);
        speed = findViewById(R.id.exo_speed);
        setupLikeAndDislike();
        setupMenu();

        playerView.setControllerVisibilityListener(visibility -> {
            if (visibility != View.VISIBLE) {
                exo_playback_menu.setVisibility(View.VISIBLE);
                exo_settings_menu.setVisibility(View.GONE);
            }
        });

        // setup media session
        mediaSession = new MediaSessionCompat(this, getPackageName());
        mediaController = new MediaSessionConnector(mediaSession);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (Util.SDK_INT > 23) {
            initializePlayer();
            mediaController.setPlayer(player);
            mediaSession.setActive(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
            mediaController.setPlayer(player);
            mediaSession.setActive(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (Util.SDK_INT <= 23) {
            mediaController.setPlayer(null);
            mediaSession.setActive(false);
            saveVideoPosition();
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (Util.SDK_INT > 23) {
            mediaController.setPlayer(null);
            mediaSession.setActive(false);
            saveVideoPosition();
            releasePlayer();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        // Hide the menu
        if (playerView.isControllerVisible()) {
            if (exo_playback_menu.getVisibility() == View.VISIBLE) {
                playerView.hideController();
            } else {
                exo_settings_menu.setVisibility(View.GONE);
                exo_playback_menu.setVisibility(View.VISIBLE);
            }
        } else {
            super.onBackPressed();
        }
    }

    private void setupLikeAndDislike() {
        client.getPost(video.getId(), post -> {
            if (!post.getUserInteractions().isEmpty()) {
                if (post.isLiked()) {
                    like.setImageResource(R.drawable.ic_like);
                } else if (post.isDisliked()) {
                    dislike.setImageResource(R.drawable.ic_dislike);
                }
            }

            return Unit.INSTANCE;
        });

        like.setOnClickListener(v -> client.toggleLikePost(video.getId(), liked -> {
            if (liked) {
                like.setImageResource(R.drawable.ic_like);
            } else {
                like.setImageResource(R.drawable.ic_like_unselected);
            }

            dislike.setImageResource(R.drawable.ic_dislike_unselected);
            return Unit.INSTANCE;
        }));
        dislike.setOnClickListener(v -> client.toggleDislikePost(video.getId(), disliked -> {
            if (disliked) {
                dislike.setImageResource(R.drawable.ic_dislike);
            } else {
                dislike.setImageResource(R.drawable.ic_dislike_unselected);
            }

            like.setImageResource(R.drawable.ic_like_unselected);
            return Unit.INSTANCE;
        }));
    }

    private void setupMenu() {
        // Show settings menu
        menu.setOnClickListener(v -> {
            exo_playback_menu.setVisibility(View.GONE);
            exo_settings_menu.setVisibility(View.VISIBLE);
        });

        speed.setOnClickListener(v -> showSpeedDialog());
    }

    private void showSpeedDialog() {
        PopupMenu speedMenu = new PopupMenu(this, speed);
        String[] playerSpeedArrayLabels = {"0.5x", "1.0x", "1.25x", "1.5x", "2.0x"};

        for (int i = 0; i < playerSpeedArrayLabels.length; i++) {
            speedMenu.getMenu().add(i, i, i, playerSpeedArrayLabels[i]);
        }

        speedMenu.setOnMenuItemClickListener(item -> {
            String itemTitle = item.getTitle().toString();
            float playbackSpeed = Float.parseFloat(itemTitle.substring(0, itemTitle.length() - 1));

            String msg = "Playback Speed: " + itemTitle;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            player.setPlaybackSpeed(playbackSpeed);
            return false;
        });

        speedMenu.show();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        playerView.setPlayer(player);
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        HashMap<String, String> cookieMap = new HashMap<>();
        cookieMap.put("Cookie", "sails.sid=" + MainFragment.sailssid + ";");
        dataSourceFactory.setDefaultRequestProperties(cookieMap);
        int flags = DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES | DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS;
        DefaultHlsExtractorFactory extractorFactory = new DefaultHlsExtractorFactory(flags, true);
        MediaItem mi = MediaItem.fromUri(url);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).setExtractorFactory(extractorFactory).createMediaSource(mi);
        player.setMediaSource(hlsMediaSource);

        player.prepare();

        player.addListener(new Player.Listener() {

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (video != null) {
                    releasePlayer();
                    Toast.makeText(PlaybackActivity.this, "Video could not be played!", Toast.LENGTH_LONG).show();
                }
                MainFragment.dError("EXOPLAYER", error.getLocalizedMessage());
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                MainFragment.dLog("STATE", state + "");
                switch (state) {
                    case Player.STATE_READY:
                        if (getIntent().getBooleanExtra(DetailsActivity.Resume, false) && !resumed) {
                            player.seekTo(video.getVideoInfo().getProgress() * 1000);
                            resumed = true;
                        }
                        break;
                    case Player.STATE_ENDED:
                        saveVideoPosition();
                        releasePlayer();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void saveVideoPosition() {
        if (player != null) {
            client.setVideoProgress(video.getVideoId(), (int) (player.getCurrentPosition() / 1000));
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            player.stop();
            player.release();
            player = null;
            this.finish();
        }
    }
}
