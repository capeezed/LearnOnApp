package com.learnon.app.ui.cursos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

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

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentTestActivity extends AppCompatActivity {

    private EditText etCourseId;
    private Button btnComprar;
    private TextView tvResultado, tvVoltar;
    private SessionManager session;
    private ApiService api;
    private long ultimoPagamentoId = 0;
    private int ultimoCursoId = 0;
    private boolean checkoutAberto = false;
    private boolean avisoCompraExibido = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_test);

        session = new SessionManager(this);
        api = ApiClient.createService(ApiService.class);

        etCourseId = findViewById(R.id.etCourseId);
        btnComprar = findViewById(R.id.btnComprar);
        tvResultado = findViewById(R.id.tvResultado);
        tvVoltar = findViewById(R.id.tvVoltar);

        tvVoltar.setOnClickListener(v -> finish());
        btnComprar.setOnClickListener(v -> criarPagamento());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkoutAberto && ultimoPagamentoId > 0 && !avisoCompraExibido) {
            consultarLiberacao();
        }
    }

    private void criarPagamento() {
        String courseIdText = etCourseId.getText().toString().trim();

        if (courseIdText.isEmpty()) {
            mostrarResultado("Informe o ID do curso.");
            return;
        }

        int courseId = Integer.parseInt(courseIdText);
        btnComprar.setEnabled(false);
        btnComprar.setText("Criando pagamento...");
        mostrarResultado("");

        criarPagamentoComToken(courseId, false);
    }

    private void criarPagamentoComToken(int courseId, boolean jaTentouRenovar) {
        String token = "Bearer " + session.getToken();

        api.criarPagamentoCurso(token, courseId).enqueue(new Callback<CoursePayment>() {
            @Override
            public void onResponse(Call<CoursePayment> call, Response<CoursePayment> response) {
                if (response.code() == 401 && !jaTentouRenovar) {
                    renovarToken(() -> criarPagamentoComToken(courseId, true));
                    return;
                }

                finalizarBotao();

                if (response.isSuccessful() && response.body() != null) {
                    CoursePayment payment = response.body();
                    ultimoPagamentoId = payment.getId();
                    ultimoCursoId = payment.getCourseId();
                    mostrarResultado(String.format(
                            Locale.getDefault(),
                            "Pagamento criado.\nStatus: %s\nValor: %.2f %s",
                            payment.getStatus(),
                            payment.getAmount(),
                            payment.getCurrency()
                    ));

                    abrirCheckout(payment.getCheckoutUrl());
                    return;
                }

                mostrarResultado("Erro ao criar pagamento. HTTP " + response.code() + "\n" + lerErro(response.errorBody()));
            }

            @Override
            public void onFailure(Call<CoursePayment> call, Throwable t) {
                finalizarBotao();
                mostrarResultado("Erro de conexao: " + t.getMessage());
            }
        });
    }

    private void renovarToken(Runnable onSuccess) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            finalizarBotao();
            mostrarResultado("Sessao expirada. Faca login novamente.");
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
                    finalizarBotao();
                    mostrarResultado("Sessao expirada. Faca login novamente.");
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                finalizarBotao();
                mostrarResultado("Erro ao renovar sessao.");
            }
        });
    }

    private void abrirCheckout(String checkoutUrl) {
        if (checkoutUrl == null || checkoutUrl.isEmpty()) {
            mostrarResultado("Pagamento criado, mas checkout_url veio vazia.");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
        checkoutAberto = true;
        startActivity(intent);
    }

    private void consultarLiberacao() {
        String token = "Bearer " + session.getToken();
        mostrarResultado("Verificando liberacao do curso...");

        api.pagamento(token, ultimoPagamentoId).enqueue(new Callback<CoursePayment>() {
            @Override
            public void onResponse(Call<CoursePayment> call, Response<CoursePayment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CoursePayment payment = response.body();
                    if ("approved".equals(payment.getStatus())) {
                        mostrarCompraAprovada();
                        return;
                    }
                }

                verificarCursoEmMeusCursos();
            }

            @Override
            public void onFailure(Call<CoursePayment> call, Throwable t) {
                verificarCursoEmMeusCursos();
            }
        });
    }

    private void verificarCursoEmMeusCursos() {
        String token = "Bearer " + session.getToken();

        api.meusCursos(token).enqueue(new Callback<List<Curso>>() {
            @Override
            public void onResponse(Call<List<Curso>> call, Response<List<Curso>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Curso curso : response.body()) {
                        if (curso.getId() == ultimoCursoId) {
                            mostrarCompraAprovada();
                            return;
                        }
                    }
                }

                mostrarResultado("Pagamento ainda pendente. Assim que for aprovado, o curso aparecera em Meus Cursos.");
            }

            @Override
            public void onFailure(Call<List<Curso>> call, Throwable t) {
                mostrarResultado("Nao foi possivel verificar agora. Confira em Meus Cursos daqui a pouco.");
            }
        });
    }

    private void mostrarCompraAprovada() {
        avisoCompraExibido = true;
        checkoutAberto = false;
        mostrarResultado("Pagamento aprovado. Curso liberado em Meus Cursos.");

        new AlertDialog.Builder(this)
                .setTitle("Curso liberado")
                .setMessage("Pagamento aprovado. Seu curso ja esta disponivel em Meus Cursos.")
                .setPositiveButton("Abrir Meus Cursos", (dialog, which) ->
                        startActivity(new Intent(this, MeusCursosActivity.class))
                )
                .setNegativeButton("Continuar", null)
                .show();
    }

    private void finalizarBotao() {
        btnComprar.setEnabled(true);
        btnComprar.setText("Comprar curso");
    }

    private void mostrarResultado(String mensagem) {
        tvResultado.setText(mensagem);
        tvResultado.setVisibility(mensagem.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String lerErro(ResponseBody errorBody) {
        if (errorBody == null) return "";

        try {
            return errorBody.string();
        } catch (Exception e) {
            return "Nao foi possivel ler a resposta de erro.";
        }
    }
}
