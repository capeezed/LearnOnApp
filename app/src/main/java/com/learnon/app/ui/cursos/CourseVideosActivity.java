package com.learnon.app.ui.cursos;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private TextView tvSemVideos, tvResumoVideos;
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
        tvResumoVideos = findViewById(R.id.tvResumoVideos);
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
                    mostrarVazio("Videos disponiveis apenas para cursos publicados e matriculados.");
                    return;
                }

                listaVideos.removeAllViews();
                List<CourseVideo> videos = response.body();
                if (videos.isEmpty()) {
                    mostrarVazio("Este curso ainda nao tem videos.");
                    return;
                }

                tvSemVideos.setVisibility(View.GONE);
                listaVideos.setVisibility(View.VISIBLE);
                tvResumoVideos.setText(videos.size() == 1
                        ? "1 aula disponivel para assistir."
                        : videos.size() + " aulas disponiveis para assistir.");

                for (CourseVideo video : videos) {
                    listaVideos.addView(criarItemVideo(video));
                }
            }

            @Override
            public void onFailure(Call<List<CourseVideo>> call, Throwable t) {
                mostrarVazio("Erro ao carregar videos.");
            }
        });
    }

    private View criarItemVideo(CourseVideo video) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.course_card_bg));
        card.setElevation(dp(2));
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(params);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, 0, 0, dp(12));

        TextView number = new TextView(this);
        number.setText(String.valueOf(video.getOrderIndex() + 1));
        number.setGravity(Gravity.CENTER);
        number.setTextColor(0xFFECEEF9);
        number.setTextSize(15);
        number.setTypeface(null, Typeface.BOLD);
        number.setBackground(ContextCompat.getDrawable(this, R.drawable.course_chip_primary_bg));
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        numberParams.setMargins(0, 0, dp(14), 0);
        number.setLayoutParams(numberParams);

        TextView title = new TextView(this);
        title.setText(textoOuPadrao(video.getTitle(), "Aula sem titulo"));
        title.setTextColor(0xFFECEEF9);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView description = new TextView(this);
        description.setText(textoOuPadrao(video.getDescription(), "Toque para assistir esta aula."));
        description.setTextColor(0xFFB4B4C3);
        description.setTextSize(13);
        description.setPadding(0, 0, 0, dp(14));
        description.setMaxLines(3);

        LinearLayout progressHeader = new LinearLayout(this);
        progressHeader.setOrientation(LinearLayout.HORIZONTAL);
        progressHeader.setGravity(Gravity.CENTER_VERTICAL);
        progressHeader.setPadding(0, 0, 0, dp(8));

        TextView progressLabel = new TextView(this);
        progressLabel.setText("Progresso");
        progressLabel.setTextColor(0xFFB4B4C3);
        progressLabel.setTextSize(13);
        progressLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        int percent = limitarProgresso(video.getPercentComplete());
        TextView progressValue = new TextView(this);
        progressValue.setText(percent + "%");
        progressValue.setTextColor(0xFFECEEF9);
        progressValue.setTextSize(13);
        progressValue.setTypeface(null, Typeface.BOLD);

        progressHeader.addView(progressLabel);
        progressHeader.addView(progressValue);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(percent);
        progressBar.setIndeterminate(false);
        progressBar.setProgressDrawable(criarProgressDrawable());
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(8)
        ));

        topRow.addView(number);
        topRow.addView(title);
        card.addView(topRow);
        card.addView(description);
        card.addView(progressHeader);
        card.addView(progressBar);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_ID, video.getId());
            startActivity(intent);
        });

        return card;
    }

    private void mostrarVazio(String mensagem) {
        listaVideos.removeAllViews();
        listaVideos.setVisibility(View.GONE);
        tvSemVideos.setText(mensagem);
        tvSemVideos.setVisibility(View.VISIBLE);
        tvResumoVideos.setText("Assim que houver aulas liberadas, elas aparecem aqui.");
    }

    private Drawable criarProgressDrawable() {
        Drawable background = ContextCompat.getDrawable(this, R.drawable.course_progress_bg);
        Drawable progress = new ClipDrawable(
                ContextCompat.getDrawable(this, R.drawable.course_progress_fill),
                Gravity.START,
                ClipDrawable.HORIZONTAL
        );
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background, progress});
        layerDrawable.setId(0, android.R.id.background);
        layerDrawable.setId(1, android.R.id.progress);
        return layerDrawable;
    }

    private int limitarProgresso(int progresso) {
        return Math.max(0, Math.min(100, progresso));
    }

    private String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.trim().isEmpty() ? padrao : texto;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
