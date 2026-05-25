package com.learnon.app.ui.cursos;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.CoursePayment;
import com.learnon.app.data.model.Curso;
import com.learnon.app.data.model.Student;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeusCursosActivity extends AppCompatActivity {

    private LinearLayout listaCursos;
    private LinearLayout emptyCursos;
    private TextView tvSemCursos, tvSemCursosDetalhe, tvVoltar, tvResumoCursos, tvSecaoCursos;
    private SessionManager session;
    private ApiService api;
    private boolean checkoutAberto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meus_cursos);

        session      = new SessionManager(this);
        api          = ApiClient.createService(ApiService.class);
        listaCursos  = findViewById(R.id.listaCursos);
        emptyCursos  = findViewById(R.id.emptyCursos);
        tvSemCursos  = findViewById(R.id.tvSemCursos);
        tvSemCursosDetalhe = findViewById(R.id.tvSemCursosDetalhe);
        tvVoltar     = findViewById(R.id.tvVoltar);
        tvResumoCursos = findViewById(R.id.tvResumoCursos);
        tvSecaoCursos = findViewById(R.id.tvSecaoCursos);

        tvVoltar.setOnClickListener(v -> finish());

        carregarCursos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkoutAberto) {
            checkoutAberto = false;
            carregarCursos();
        }
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
                        mostrarEstadoVazio("Voce ainda nao tem cursos.",
                                "Quando voce se matricular, eles aparecem aqui com progresso e acesso rapido.");
                        return;
                    }

                    mostrarLista(cursos);

                    for (Curso curso : cursos) {
                        listaCursos.addView(criarItemCurso(curso));
                    }
                } else {
                    mostrarEstadoVazio("Nao foi possivel carregar seus cursos.",
                            "Tente novamente em alguns instantes.");
                }
            }

            @Override
            public void onFailure(Call<List<Curso>> call, Throwable t) {
                mostrarEstadoVazio("Erro ao carregar cursos.",
                        "Verifique sua conexao e tente novamente.");
            }
        });
    }

    private void renovarToken(Runnable onSuccess) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            mostrarEstadoVazio("Sessao expirada.",
                    "Faca login novamente para ver seus cursos.");
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
                    mostrarEstadoVazio("Sessao expirada.",
                            "Faca login novamente para ver seus cursos.");
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                mostrarEstadoVazio("Erro ao renovar sessao.",
                        "Entre novamente para continuar aprendendo.");
            }
        });
    }

    private View criarItemCurso(Curso curso) {
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

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, 0, 0, dp(14));

        TextView titulo = new TextView(this);
        titulo.setText(textoOuPadrao(curso.getTitle(), "Curso sem titulo"));
        titulo.setTextSize(17);
        titulo.setTextColor(0xFFECEEF9);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setMaxLines(2);
        titulo.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView seta = new TextView(this);
        seta.setText(">");
        seta.setTextSize(20);
        seta.setTextColor(0xFFB4B4C3);
        seta.setTypeface(null, Typeface.BOLD);
        seta.setGravity(Gravity.CENTER);
        seta.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));

        topRow.addView(titulo);
        topRow.addView(seta);

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(0, 0, 0, dp(16));

        TextView topico = criarChip(textoOuPadrao(curso.getTopicTag(), "Geral"), false);
        TextView formato = criarChip(isAoVivo(curso) ? "Ao vivo" : "Gravado", true);
        TextView duracao = criarChip(formatarDuracao(curso.getDurationMinutes()), false);
        TextView acesso = criarChip(curso.isPaymentRequired() ? statusPagamento(curso) : "Liberado", curso.isPaymentRequired());

        chipRow.addView(topico);
        chipRow.addView(formato);
        chipRow.addView(duracao);
        chipRow.addView(acesso);

        LinearLayout progressHeader = new LinearLayout(this);
        progressHeader.setOrientation(LinearLayout.HORIZONTAL);
        progressHeader.setGravity(Gravity.CENTER_VERTICAL);
        progressHeader.setPadding(0, 0, 0, dp(8));

        TextView progressoLabel = new TextView(this);
        progressoLabel.setText("Progresso");
        progressoLabel.setTextSize(13);
        progressoLabel.setTextColor(0xFFB4B4C3);
        progressoLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView progressoValor = new TextView(this);
        int progresso = limitarProgresso(curso.getProgress());
        progressoValor.setText(progresso + "%");
        progressoValor.setTextSize(13);
        progressoValor.setTextColor(0xFFECEEF9);
        progressoValor.setTypeface(null, Typeface.BOLD);

        progressHeader.addView(progressoLabel);
        progressHeader.addView(progressoValor);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(progresso);
        progressBar.setProgressDrawable(criarProgressDrawable());
        progressBar.setIndeterminate(false);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8)
        ));

        card.addView(topRow);
        card.addView(chipRow);
        if (curso.isPaymentRequired()) {
            TextView payInfo = new TextView(this);
            payInfo.setText(String.format(
                    Locale.getDefault(),
                    "Curso pronto. Pague R$ %.2f para liberar as aulas.",
                    curso.getPrice()
            ));
            payInfo.setTextSize(13);
            payInfo.setTextColor(0xFFB4B4C3);
            payInfo.setPadding(0, 0, 0, dp(14));

            TextView payButton = new TextView(this);
            payButton.setText("Pagar e liberar acesso");
            payButton.setTextColor(0xFFFFFFFF);
            payButton.setTextSize(14);
            payButton.setTypeface(null, Typeface.BOLD);
            payButton.setGravity(Gravity.CENTER);
            payButton.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_primary));
            payButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            ));
            payButton.setOnClickListener(v -> iniciarPagamento(curso));

            card.addView(payInfo);
            card.addView(payButton);
        } else {
            card.addView(progressHeader);
            card.addView(progressBar);
        }

        card.setOnClickListener(v -> {
            if (curso.isPaymentRequired()) {
                iniciarPagamento(curso);
                return;
            }
            Intent intent = new Intent(this, CourseVideosActivity.class);
            intent.putExtra(CourseVideosActivity.EXTRA_COURSE_ID, curso.getId());
            intent.putExtra(CourseVideosActivity.EXTRA_COURSE_TITLE, curso.getTitle());
            startActivity(intent);
        });

        return card;
    }

    private void iniciarPagamento(Curso curso) {
        if ("processing".equals(curso.getAccessStatus()) || "approved".equals(curso.getPaymentStatus())) {
            new AlertDialog.Builder(this)
                    .setTitle("Pagamento em processamento")
                    .setMessage("Recebemos o pagamento e estamos aguardando a liberacao automatica. Atualize a tela em alguns instantes.")
                    .setPositiveButton("Atualizar", (dialog, which) -> carregarCursos())
                    .setNegativeButton("Fechar", null)
                    .show();
            return;
        }

        criarPagamentoCurso(curso.getId(), false);
    }

    private void criarPagamentoCurso(int courseId, boolean jaTentouRenovar) {
        String token = "Bearer " + session.getToken();

        api.criarPagamentoCurso(token, courseId).enqueue(new Callback<CoursePayment>() {
            @Override
            public void onResponse(Call<CoursePayment> call, Response<CoursePayment> response) {
                if (response.code() == 401 && !jaTentouRenovar) {
                    renovarToken(() -> criarPagamentoCurso(courseId, true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    abrirCheckout(response.body().getCheckoutUrl());
                    return;
                }

                new AlertDialog.Builder(MeusCursosActivity.this)
                        .setTitle("Nao foi possivel iniciar o pagamento")
                        .setMessage("Tente novamente em alguns instantes.")
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onFailure(Call<CoursePayment> call, Throwable t) {
                new AlertDialog.Builder(MeusCursosActivity.this)
                        .setTitle("Erro de conexao")
                        .setMessage("Nao foi possivel criar o pagamento agora.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void abrirCheckout(String checkoutUrl) {
        if (checkoutUrl == null || checkoutUrl.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Checkout indisponivel")
                    .setMessage("O pagamento foi criado, mas o link de checkout nao foi retornado.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        checkoutAberto = true;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl)));
    }

    private void mostrarLista(List<Curso> cursos) {
        emptyCursos.setVisibility(View.GONE);
        listaCursos.setVisibility(View.VISIBLE);
        tvSecaoCursos.setText("Cursos em andamento");
        tvResumoCursos.setText(criarResumo(cursos));
    }

    private void mostrarEstadoVazio(String titulo, String detalhe) {
        listaCursos.removeAllViews();
        listaCursos.setVisibility(View.GONE);
        emptyCursos.setVisibility(View.VISIBLE);
        tvSecaoCursos.setText("Sua lista");
        tvResumoCursos.setText("Tudo pronto para sua proxima matricula.");
        tvSemCursos.setText(titulo);
        tvSemCursosDetalhe.setText(detalhe);
    }

    private String criarResumo(List<Curso> cursos) {
        int total = cursos.size();
        int soma = 0;
        int bloqueados = 0;
        for (Curso curso : cursos) {
            soma += limitarProgresso(curso.getProgress());
            if (curso.isPaymentRequired()) bloqueados++;
        }
        int media = total == 0 ? 0 : Math.round((float) soma / total);
        String textoCursos = total == 1 ? "1 curso ativo" : total + " cursos ativos";
        if (bloqueados > 0) {
            return textoCursos + " | " + bloqueados + " aguardando pagamento";
        }
        return textoCursos + " | media de progresso: " + media + "%";
    }

    private TextView criarChip(String texto, boolean destaque) {
        TextView chip = new TextView(this);
        chip.setText(texto);
        chip.setTextSize(12);
        chip.setTextColor(destaque ? 0xFFECEEF9 : 0xFFB4B4C3);
        chip.setTypeface(null, destaque ? Typeface.BOLD : Typeface.NORMAL);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setBackground(ContextCompat.getDrawable(this,
                destaque ? R.drawable.course_chip_primary_bg : R.drawable.course_chip_soft_bg));
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
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

    private boolean isAoVivo(Curso curso) {
        return "live".equalsIgnoreCase(curso.getFormat());
    }

    private int limitarProgresso(int progresso) {
        return Math.max(0, Math.min(100, progresso));
    }

    private String formatarDuracao(int minutos) {
        if (minutos <= 0) {
            return "Duracao livre";
        }
        if (minutos < 60) {
            return minutos + " min";
        }
        int horas = minutos / 60;
        int resto = minutos % 60;
        return resto == 0 ? horas + "h" : horas + "h " + resto + "min";
    }

    private String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.trim().isEmpty() ? padrao : texto;
    }

    private String statusPagamento(Curso curso) {
        String status = curso.getPaymentStatus();
        if ("pending".equals(status) || "in_process".equals(status)) {
            return "Pagamento pendente";
        }
        if ("approved".equals(status) || "processing".equals(curso.getAccessStatus())) {
            return "Liberando acesso";
        }
        return "Pagamento necessario";
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
