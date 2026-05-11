package com.learnon.app.ui.cursos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.CourseVideo;
import com.learnon.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseVideosActivity extends AppCompatActivity {
    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_TITLE = "course_title";

    private LinearLayout listaVideos;
    private TextView tvSemVideos;
    private ApiService api;
    private SessionManager session;
    private int courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_videos);

        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, 0);
        String courseTitle = getIntent().getStringExtra(EXTRA_COURSE_TITLE);
        session = new SessionManager(this);
        api = ApiClient.createService(ApiService.class);

        listaVideos = findViewById(R.id.listaVideos);
        tvSemVideos = findViewById(R.id.tvSemVideos);
        TextView tvTituloCurso = findViewById(R.id.tvTituloCurso);
        TextView tvVoltar = findViewById(R.id.tvVoltar);

        tvTituloCurso.setText(courseTitle == null || courseTitle.isEmpty() ? "Videos do curso" : courseTitle);
        tvVoltar.setOnClickListener(v -> finish());

        carregarVideos();
    }

    private void carregarVideos() {
        api.videosDoCurso("Bearer " + session.getToken(), courseId).enqueue(new Callback<List<CourseVideo>>() {
            @Override
            public void onResponse(Call<List<CourseVideo>> call, Response<List<CourseVideo>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    tvSemVideos.setText("Videos disponiveis apenas para cursos publicados e matriculados.");
                    return;
                }

                listaVideos.removeAllViews();
                List<CourseVideo> videos = response.body();
                if (videos.isEmpty()) {
                    tvSemVideos.setText("Este curso ainda nao tem videos.");
                    listaVideos.addView(tvSemVideos);
                    return;
                }

                for (CourseVideo video : videos) {
                    listaVideos.addView(criarItemVideo(video));
                }
            }

            @Override
            public void onFailure(Call<List<CourseVideo>> call, Throwable t) {
                tvSemVideos.setText("Erro ao carregar videos.");
            }
        });
    }

    private View criarItemVideo(CourseVideo video) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(getDrawable(R.drawable.card_bg));
        card.setPadding(40, 32, 40, 32);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText((video.getOrderIndex() + 1) + ". " + video.getTitle());
        title.setTextColor(0xFFECEEF9);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView description = new TextView(this);
        description.setText(video.getDescription() == null ? "" : video.getDescription());
        description.setTextColor(0xFFB4B4C3);
        description.setTextSize(13);
        description.setPadding(0, 8, 0, 8);

        TextView progress = new TextView(this);
        progress.setText("Progresso: " + video.getPercentComplete() + "%");
        progress.setTextColor(0xFF6B5CFF);
        progress.setTextSize(13);

        card.addView(title);
        card.addView(description);
        card.addView(progress);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_ID, video.getId());
            startActivity(intent);
        });

        return card;
    }
}
