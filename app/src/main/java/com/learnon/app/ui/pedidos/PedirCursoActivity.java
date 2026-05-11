package com.learnon.app.ui.pedidos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.learnon.app.R;
import com.learnon.app.data.api.ApiClient;
import com.learnon.app.data.api.ApiService;
import com.learnon.app.data.model.Pedido;
import com.learnon.app.data.model.Student;
import com.learnon.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PedirCursoActivity extends AppCompatActivity {

    private EditText etTitulo, etDescricao, etTopico;
    private Button btnAoVivo, btnGravado, btnEnviar;
    private TextView tvErro, tvSucesso, tvVoltar;
    private SessionManager session;
    private ApiService api;
    private String formatoSelecionado = "no_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedir_curso);

        session      = new SessionManager(this);
        api          = ApiClient.createService(ApiService.class);

        etTitulo     = findViewById(R.id.etTitulo);
        etDescricao  = findViewById(R.id.etDescricao);
        etTopico     = findViewById(R.id.etTopico);
        btnAoVivo    = findViewById(R.id.btnAoVivo);
        btnGravado   = findViewById(R.id.btnGravado);
        btnEnviar    = findViewById(R.id.btnEnviar);
        tvErro       = findViewById(R.id.tvErro);
        tvSucesso    = findViewById(R.id.tvSucesso);
        tvVoltar     = findViewById(R.id.tvVoltar);

        tvVoltar.setOnClickListener(v -> finish());
        atualizarFormatoSelecionado();

        btnAoVivo.setOnClickListener(v -> {
            formatoSelecionado = "live";
            atualizarFormatoSelecionado();
        });

        btnGravado.setOnClickListener(v -> {
            formatoSelecionado = "recorded";
            atualizarFormatoSelecionado();
        });

        btnEnviar.setOnClickListener(v -> enviarPedido());
    }

    private void atualizarFormatoSelecionado() {
        btnAoVivo.setSelected("live".equals(formatoSelecionado));
        btnGravado.setSelected("recorded".equals(formatoSelecionado));
    }

    private void enviarPedido() {
        String titulo    = etTitulo.getText().toString().trim();
        String descricao = etDescricao.getText().toString().trim();
        String topico    = etTopico.getText().toString().trim();

        if (titulo.isEmpty() || descricao.isEmpty() || topico.isEmpty()) {
            mostrarErro("Preencha todos os campos.");
            return;
        }

        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");
        tvErro.setVisibility(View.GONE);
        tvSucesso.setVisibility(View.GONE);

        Map<String, String> body = new HashMap<>();
        body.put("title", titulo);
        body.put("description", descricao);
        body.put("topic_tag", topico);
        body.put("format_preference", formatoSelecionado);
        body.put("urgency", "normal");

        enviarPedidoComToken(body, false);
    }

    private void enviarPedidoComToken(Map<String, String> body, boolean jaTentouRenovar) {
        String token = "Bearer " + session.getToken();

        api.criarPedido(token, body).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> call, Response<Pedido> response) {
                if (response.code() == 401 && !jaTentouRenovar) {
                    renovarToken(() -> enviarPedidoComToken(body, true));
                    return;
                }

                finalizarEnvio();

                if (response.isSuccessful()) {
                    etTitulo.setText("");
                    etDescricao.setText("");
                    etTopico.setText("");
                    tvSucesso.setText("Pedido enviado com sucesso!");
                    tvSucesso.setVisibility(View.VISIBLE);
                } else {
                    mostrarErro("Erro ao enviar pedido. Tente novamente.");
                }
            }

            @Override
            public void onFailure(Call<Pedido> call, Throwable t) {
                finalizarEnvio();
                mostrarErro("Erro de conexao. Tente novamente.");
            }
        });
    }

    private void renovarToken(Runnable onSuccess) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            finalizarEnvio();
            mostrarErro("Sessao expirada. Faca login novamente.");
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
                    finalizarEnvio();
                    mostrarErro("Sessao expirada. Faca login novamente.");
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {
                finalizarEnvio();
                mostrarErro("Erro ao renovar sessao. Tente novamente.");
            }
        });
    }

    private void finalizarEnvio() {
        btnEnviar.setEnabled(true);
        btnEnviar.setText("Enviar pedido");
    }

    private void mostrarErro(String msg) {
        tvErro.setText(msg);
        tvErro.setVisibility(View.VISIBLE);
    }
}
