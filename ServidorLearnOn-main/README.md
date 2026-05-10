# LearnOn API

Backend em Node.js + Express usando SQL direto com `mysql2`, seguindo a estrutura existente do projeto.

## Setup

1. Configure a conexao da VM no `.env`:

```env
DB_HOST=
DB_PORT=
DB_USER=
DB_PASSWORD=
DB_NAME=learnon
```

2. Instale dependencias:

```bash
npm install
```

3. Rode manualmente na VM os scripts SQL em `database/migrations`.

4. Inicie:

```bash
npm run dev
```

## Modulos

- Autenticacao JWT com access token, refresh token e roles: `aluno`, `instrutor`, `admin`.
- Pedidos de curso com status, urgencia, fila normal e fila fast-track.
- Prioridade isolada em `src/services/priorityService.js`, com testes unitarios.
- Alteracoes de banco documentadas como scripts SQL em `database/migrations`.

Exemplos de payloads em `docs/api-examples.md`.
