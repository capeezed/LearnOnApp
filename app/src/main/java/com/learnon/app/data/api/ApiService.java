package com.learnon.app.data.api;

import com.learnon.app.data.model.Aula;
import com.learnon.app.data.model.Curso;
import com.learnon.app.data.model.Pedido;
import com.learnon.app.data.model.Student;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    @POST("auth/students/register")
    Call<Student> register(@Body Map<String, String> body);

    @POST("auth/students/login")
    Call<Student> login(@Body Map<String, String> body);

    @GET("requests")
    Call<List<Pedido>> meusPedidos(@Header("Authorization") String token);

    @POST("requests")
    Call<Pedido> criarPedido(@Header("Authorization") String token, @Body Map<String, String> body);

    @GET("courses/my")
    Call<List<Curso>> meusCursos(@Header("Authorization") String token);

    @GET("schedules")
    Call<List<Aula>> minhaAgenda(@Header("Authorization") String token);
}