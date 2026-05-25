# LearnOn API - exemplos JSON

## Registrar aluno

`POST /api/auth/students/register`

```json
{
  "name": "Maria Silva",
  "email": "maria@learnon.com",
  "password": "senha-forte-123"
}
```

## Login aluno

`POST /api/auth/students/login`

```json
{
  "email": "maria@learnon.com",
  "password": "senha-forte-123"
}
```

## Renovar token

`POST /api/auth/refresh`

```json
{
  "refreshToken": "refresh-token-retornado-no-login"
}
```

## Registrar instrutor

`POST /api/auth/instructors/register`

```json
{
  "name": "Joao Instrutor",
  "email": "joao@learnon.com",
  "password": "senha-forte-123",
  "bio": "Especialista em backend Node.js.",
  "expertise": ["node.js", "sql", "backend"]
}
```

## Criar pedido de curso

`POST /api/requests`

Headers:

```json
{
  "Authorization": "Bearer access-token"
}
```

Body:

```json
{
  "title": "Como implementar refresh token com JWT?",
  "description": "Tenho duvida sobre rotacao de refresh token, revogacao e expiracao segura em uma API Express.",
  "category": "node.js",
  "format_preference": "live",
  "urgency": "fast_track"
}
```

Valores aceitos:

```json
{
  "format_preference": ["live", "recorded", "no_preference"],
  "urgency": ["normal", "fast_track"],
  "status": ["aguardando_match", "aguardando_instrutor", "em_andamento", "concluido", "cancelado"]
}
```

## Listar fila de prioridade

`GET /api/requests/queue?queue=fast_track&limit=20`

Fast-track fica separado de `queue=normal`.
