package com.learnon.app.ui.agenda;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private TextView tvSemAulas, tvVoltar, tvResumoAgenda;
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
        tvResumoAgenda = findViewById(R.id.tvResumoAgenda);

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
                        mostrarVazio("Nenhuma aula agendada.");
                        return;
                    }

                    tvSemAulas.setVisibility(View.GONE);
                    listaAulas.setVisibility(View.VISIBLE);
                    tvResumoAgenda.setText(aulas.size() == 1
                            ? "1 aula marcada na sua agenda."
                            : aulas.size() + " aulas marcadas na sua agenda.");

                    for (Aula aula : aulas) {
                        listaAulas.addView(criarItemAula(aula));
                    }
                } else {
                    mostrarVazio("Nao foi possivel carregar sua agenda.");
                }
            }

            @Override
            public void onFailure(Call<List<Aula>> call, Throwable t) {
                mostrarVazio("Erro ao carregar agenda.");
            }
        });
    }

    private void renovarToken(Runnable onSuccess) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            mostrarVazio("Sessao expirada. Faca login novamente.");
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
                    mostrarVazio("Sessao expirada. Faca login novamente.");
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                mostrarVazio("Erro ao renovar sessao.");
            }
        });
    }

    private View criarItemAula(Aula aula) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.course_card_bg));
        card.setElevation(dp(2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(params);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView titulo = new TextView(this);
        titulo.setText(textoOuPadrao(aula.getCourseTitle(), "Aula sem titulo"));
        titulo.setTextSize(17);
        titulo.setTextColor(0xFFECEEF9);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setPadding(0, 0, 0, dp(10));

        TextView instrutor = criarLinhaInfo("Instrutor", textoOuPadrao(aula.getInstructorName(), "A definir"));
        TextView horario = criarLinhaInfo("Data", formatarData(aula.getScheduledAt()));
        TextView duracao = criarLinhaInfo("Duracao", aula.getDurationMin() + " min");

        card.addView(titulo);
        card.addView(instrutor);
        card.addView(horario);
        card.addView(duracao);

        if (aula.getMeetingUrl() != null && !aula.getMeetingUrl().isEmpty()) {
            Button btnEntrar = new Button(this);
            btnEntrar.setText("Entrar na aula");
            btnEntrar.setTextColor(0xFFFFFFFF);
            btnEntrar.setTextSize(14);
            btnEntrar.setTypeface(null, Typeface.BOLD);
            btnEntrar.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_primary));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
            );
            btnParams.setMargins(0, dp(14), 0, 0);
            btnEntrar.setLayoutParams(btnParams);
            btnEntrar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(aula.getMeetingUrl()));
                startActivity(intent);
            });
            card.addView(btnEntrar);
        }

        return card;
    }

    private TextView criarLinhaInfo(String rotulo, String valor) {
        TextView text = new TextView(this);
        text.setText(rotulo + ": " + valor);
        text.setTextSize(13);
        text.setTextColor(0xFFB4B4C3);
        text.setPadding(0, 0, 0, dp(5));
        return text;
    }

    private void mostrarVazio(String mensagem) {
        listaAulas.removeAllViews();
        listaAulas.setVisibility(View.GONE);
        tvSemAulas.setText(mensagem);
        tvSemAulas.setVisibility(View.VISIBLE);
        tvResumoAgenda.setText("Quando houver aulas marcadas, elas aparecem aqui.");
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

    private String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.trim().isEmpty() ? padrao : texto;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
