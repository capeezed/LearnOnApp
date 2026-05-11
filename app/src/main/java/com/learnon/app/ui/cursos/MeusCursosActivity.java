package com.learnon.app.ui.cursos;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Curso;
import com.learnon.app.data.model.Student;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeusCursosActivity extends AppCompatActivity {

    private LinearLayout listaCursos;
    private TextView tvSemCursos, tvVoltar;
    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meus_cursos);

        session      = new SessionManager(this);
        api          = ApiClient.createService(ApiService.class);
        listaCursos  = findViewById(R.id.listaCursos);
        tvSemCursos  = findViewById(R.id.tvSemCursos);
        tvVoltar     = findViewById(R.id.tvVoltar);

        tvVoltar.setOnClickListener(v -> finish());

        carregarCursos();
    }

    private void carregarCursos() {
        carregarCursosComToken(false);
    }

    private void carregarCursosComToken(boolean jaTentouRenovar) {
        String token = "Bearer " + session.getToken();

        api.meusCursos(token).enqueue(new Callback<List<Curso>>() {
            @Override
            public void onResponse(Call<List<Curso>> call, Response<List<Curso>> response) {
                if (response.code() == 401 && !jaTentouRenovar) {
                    renovarToken(() -> carregarCursosComToken(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<Curso> cursos = response.body();
                    listaCursos.removeAllViews();

                    if (cursos.isEmpty()) {
                        tvSemCursos.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvSemCursos.setVisibility(View.GONE);

                    for (Curso curso : cursos) {
                        listaCursos.addView(criarItemCurso(curso));
                    }
                } else {
                    tvSemCursos.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Curso>> call, Throwable t) {
                tvSemCursos.setVisibility(View.VISIBLE);
                tvSemCursos.setText("Erro ao carregar cursos.");
            }
        });
    }

    private void renovarToken(Runnable onSuccess) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            tvSemCursos.setVisibility(View.VISIBLE);
            tvSemCursos.setText("Sessao expirada. Faca login novamente.");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        api.refresh(body).enqueue(new Callback<Student>() {
            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Student student = response.body();
                    session.salvarTokens(student.getToken(), student.getRefreshToken());
                    onSuccess.run();
                } else {
                    tvSemCursos.setVisibility(View.VISIBLE);
                    tvSemCursos.setText("Sessao expirada. Faca login novamente.");
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                tvSemCursos.setVisibility(View.VISIBLE);
                tvSemCursos.setText("Erro ao renovar sessao.");
            }
        });
    }

    private View criarItemCurso(Curso curso) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(getDrawable(R.drawable.card_bg));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setPadding(40, 32, 40, 32);

        TextView titulo = new TextView(this);
        titulo.setText(curso.getTitle());
        titulo.setTextSize(16);
        titulo.setTextColor(0xFFECEEF9);
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        titulo.setPadding(0, 0, 0, 6);

        TextView topico = new TextView(this);
        topico.setText(curso.getTopicTag());
        topico.setTextSize(12);
        topico.setTextColor(0xFFB4B4C3);
        topico.setPadding(0, 0, 0, 12);

        TextView formato = new TextView(this);
        formato.setText(curso.getFormat().equals("live") ? "Ao vivo" : "Gravado");
        formato.setTextSize(12);
        formato.setTextColor(curso.getFormat().equals("live") ? 0xFF6B5CFF : 0xFF4937A6);
        formato.setPadding(0, 0, 0, 12);

        TextView progresso = new TextView(this);
        progresso.setText("Progresso: " + curso.getProgress() + "%");
        progresso.setTextSize(13);
        progresso.setTextColor(0xFFECEEF9);

        card.addView(titulo);
        card.addView(topico);
        card.addView(formato);
        card.addView(progresso);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, CourseVideosActivity.class);
            intent.putExtra(CourseVideosActivity.EXTRA_COURSE_ID, curso.getId());
            intent.putExtra(CourseVideosActivity.EXTRA_COURSE_TITLE, curso.getTitle());
            startActivity(intent);
        });

        return card;
    }
}
