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

        btnAoVivo.setOnClickListener(v -> {
            formatoSelecionado = "live";
            btnAoVivo.setBackground(getDrawable(R.drawable.btn_primary));
            btnAoVivo.setTextColor(0xFFffffff);
            btnGravado.setBackground(getDrawable(R.drawable.btn_outline));
            btnGravado.setTextColor(0xFF1c2b3a);
        });

        btnGravado.setOnClickListener(v -> {
            formatoSelecionado = "recorded";
            btnGravado.setBackground(getDrawable(R.drawable.btn_primary));
            btnGravado.setTextColor(0xFFffffff);
            btnAoVivo.setBackground(getDrawable(R.drawable.btn_outline));
            btnAoVivo.setTextColor(0xFF1c2b3a);
        });

        btnEnviar.setOnClickListener(v -> enviarPedido());
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

        String token = "Bearer " + session.getToken();

        api.criarPedido(token, body).enqueue(new Callback<Pedido>() {
            @Override
            public void onResponse(Call<Pedido> call, Response<Pedido> response) {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar pedido");

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
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar pedido");
                mostrarErro("Erro de conexao. Tente novamente.");
            }
        });
    }

    private void mostrarErro(String msg) {
        tvErro.setText(msg);
        tvErro.setVisibility(View.VISIBLE);
    }
}