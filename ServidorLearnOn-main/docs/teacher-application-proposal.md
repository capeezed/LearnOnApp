# Candidatura de instrutor

Implementacao backend para conectar a tela Android `TeacherRegistrationScreen` ao LearnOn.

## Endpoints

`POST /api/teacher-applications`

Publico. Cria uma candidatura com status `pending`.

`GET /api/teacher-applications?status=pending&limit=50`

Restrito a `admin`. Lista candidaturas.

`PATCH /api/teacher-applications/:id/status`

Restrito a `admin`. Aprova ou rejeita candidaturas. Ao aprovar, cria ou atualiza a conta em `instructors` e copia areas para `instructor_expertise`.

Payload de aprovacao:

```json
{
  "status": "approved",
  "review_notes": "Perfil validado.",
  "temporary_password": "senha-temporaria-123"
}
```

Se `temporary_password` nao for enviado, o backend usa `DEFAULT_INSTRUCTOR_TEMP_PASSWORD` ou `senha-teste-123`.

## Payload JSON

```json
{
  "full_name": "Gabriel Instrutor",
  "email": "gabriel@learnon.com",
  "phone": "+55 11 99999-9999",
  "profile_photo_uri": "content://profile/photo",
  "bio": "Instrutor Android com experiencia em Kotlin, Compose e arquitetura mobile.",
  "location": "Sao Paulo/SP",
  "knowledge_areas": ["Android", "Backend", "Dados"],
  "subjects": "Kotlin, Jetpack Compose, REST APIs, SQL",
  "experience_level": "Senior",
  "years_experience": 6,
  "linkedin_url": "https://linkedin.com/in/gabriel",
  "github_url": "https://github.com/gabriel",
  "portfolio_url": "https://gabriel.dev",
  "class_format": "Ambos",
  "weekly_availability": ["Noites", "Fins de semana"],
  "suggested_price_range": "R$ 100-200",
  "average_response_time": "Mesmo dia",
  "document_uri": "content://document/id",
  "certificate_uri": "content://certificate/id",
  "accepted_terms": true
}
```

## Migration

Rode `database/migrations/002_teacher_applications.sql` na VM.

## Fluxo de aprovacao

1. Candidato envia a candidatura.
2. Admin revisa documentos e certificados.
3. Admin aprova ou rejeita a candidatura.
4. Ao aprovar, o backend cria a conta em `instructors`.
5. Areas selecionadas viram linhas em `instructor_expertise`, liberando o match automatico.
