package com.learnon.app.ui.agenda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Aula;
import com.learnon.app.data.model.Student;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgendaActivity extends AppCompatActivity {

    private LinearLayout listaAulas;
    private TextView tvSemAulas, tvVoltar;
    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        session    = new SessionManager(this);
        api        = ApiClient.createService(ApiService.class);
        listaAulas = findViewById(R.id.listaAulas);
        tvSemAulas = findViewById(R.id.tvSemAulas);
        tvVoltar   = findViewById(R.id.tvVoltar);

        tvVoltar.setOnClickListener(v -> finish());

        carregarAgenda();
    }

    private void carregarAgenda() {
        carregarAgendaComToken(false);
    }

    private void carregarAgendaComToken(boolean jaTentouRenovar) {
        String token = "Bearer " + session.getToken();

        api.minhaAgenda(token).enqueue(new Callback<List<Aula>>() {
            @Override
            public void onResponse(Call<List<Aula>> call, Response<List<Aula>> response) {
                if (response.code() == 401 && !jaTentouRenovar) {
                    renovarToken(() -> carregarAgendaComToken(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<Aula> aulas = response.body();
                    listaAulas.removeAllViews();

                    if (aulas.isEmpty()) {
                        tvSemAulas.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvSemAulas.setVisibility(View.GONE);

                    for (Aula aula : aulas) {
                        listaAulas.addView(criarItemAula(aula));
                    }
                } else {
                    tvSemAulas.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Aula>> call, Throwable t) {
                tvSemAulas.setVisibility(View.VISIBLE);
                tvSemAulas.setText("Erro ao carregar agenda.");
            }
        });
    }

    private void renovarToken(Runnable onSuccess) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            tvSemAulas.setVisibility(View.VISIBLE);
            tvSemAulas.setText("Sessao expirada. Faca login novamente.");
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
                    tvSemAulas.setVisibility(View.VISIBLE);
                    tvSemAulas.setText("Sessao expirada. Faca login novamente.");
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                tvSemAulas.setVisibility(View.VISIBLE);
                tvSemAulas.setText("Erro ao renovar sessao.");
            }
        });
    }

    private View criarItemAula(Aula aula) {
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
        titulo.setText(aula.getCourseTitle());
        titulo.setTextSize(16);
        titulo.setTextColor(0xFFECEEF9);
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        titulo.setPadding(0, 0, 0, 6);

        TextView instrutor = new TextView(this);
        instrutor.setText("Instrutor: " + aula.getInstructorName());
        instrutor.setTextSize(13);
        instrutor.setTextColor(0xFFB4B4C3);
        instrutor.setPadding(0, 0, 0, 4);

        TextView horario = new TextView(this);
        horario.setText("Data: " + formatarData(aula.getScheduledAt()));
        horario.setTextSize(13);
        horario.setTextColor(0xFFB4B4C3);
        horario.setPadding(0, 0, 0, 4);

        TextView duracao = new TextView(this);
        duracao.setText("Duracao: " + aula.getDurationMin() + " min");
        duracao.setTextSize(13);
        duracao.setTextColor(0xFFB4B4C3);
        duracao.setPadding(0, 0, 0, 16);

        card.addView(titulo);
        card.addView(instrutor);
        card.addView(horario);
        card.addView(duracao);

        if (aula.getMeetingUrl() != null && !aula.getMeetingUrl().isEmpty()) {
            Button btnEntrar = new Button(this);
            btnEntrar.setText("Entrar na aula");
            btnEntrar.setTextColor(0xFFFFFFFF);
            btnEntrar.setBackground(getDrawable(R.drawable.btn_primary));
            btnEntrar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(aula.getMeetingUrl()));
                startActivity(intent);
            });
            card.addView(btnEntrar);
        }

        return card;
    }

    private String formatarData(String data) {
        try {
            SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat saida   = new SimpleDateFormat("dd/MM/yyyy 'as' HH:mm", Locale.getDefault());
            Date date = entrada.parse(data);
            return saida.format(date);
        } catch (ParseException e) {
            return data;
        }
    }
}
