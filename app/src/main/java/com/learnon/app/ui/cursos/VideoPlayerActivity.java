package com.learnon.app.ui.cursos;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.CourseVideo;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_VIDEO_ID = "video_id";

    private ApiService api;
    private SessionManager session;
    private PlayerView playerView;
    private ExoPlayer player;
    private CourseVideo currentVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        api = ApiClient.createService(ApiService.class);
        session = new SessionManager(this);
        playerView = findViewById(R.id.playerView);
        findViewById(R.id.tvVoltar).setOnClickListener(v -> finish());

        int videoId = getIntent().getIntExtra(EXTRA_VIDEO_ID, 0);
        carregarVideo(videoId);
    }

    private void carregarVideo(int videoId) {
        api.video("Bearer " + session.getToken(), videoId).enqueue(new Callback<CourseVideo>() {
            @Override
            public void onResponse(Call<CourseVideo> call, Response<CourseVideo> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                currentVideo = response.body();
                ((TextView) findViewById(R.id.tvTituloVideo)).setText(currentVideo.getTitle());
                ((TextView) findViewById(R.id.tvDescricao)).setText(currentVideo.getDescription());
                iniciarPlayer();
            }

            @Override
            public void onFailure(Call<CourseVideo> call, Throwable t) {
            }
        });
    }

    private void iniciarPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        MediaItem item = MediaItem.fromUri(Uri.parse(currentVideo.getVideoUrl()));
        player.setMediaItem(item);
        player.prepare();
        int lastPositionMs = Math.max(0, currentVideo.getLastPositionSec()) * 1000;
        if (lastPositionMs > 0) player.seekTo(lastPositionMs);
        player.play();
    }

    private void salvarProgresso() {
        if (player == null || currentVideo == null) return;
        long durationMs = player.getDuration();
        long positionMs = Math.max(0, player.getCurrentPosition());
        int totalSeconds = durationMs > 0 ? (int) (durationMs / 1000) : currentVideo.getDuration();
        int watchedSeconds = (int) (positionMs / 1000);

        Map<String, Object> body = new HashMap<>();
        body.put("watched_seconds", watchedSeconds);
        body.put("total_seconds", totalSeconds);
        body.put("last_position_sec", watchedSeconds);
        api.salvarProgressoVideo("Bearer " + session.getToken(), currentVideo.getId(), body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
            }
        });
    }

    @Override
    protected void onPause() {
        salvarProgresso();
        if (player != null) player.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        salvarProgresso();
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
