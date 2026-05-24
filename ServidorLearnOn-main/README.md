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

4. Configure pagamentos em modo sandbox no `.env`:

```env
PUBLIC_API_URL=https://seu-dominio-ou-ngrok.com
MERCADO_PAGO_ACCESS_TOKEN=TEST-SEU_ACCESS_TOKEN
MERCADO_PAGO_SANDBOX=true
MERCADO_PAGO_NOTIFICATION_URL=https://seu-dominio-ou-ngrok.com/api/payments/webhooks/mercado-pago
MERCADO_PAGO_WEBHOOK_SECRET=segredo_do_webhook
```

Use credenciais `TEST-...` para sandbox e `APP_USR-...` em producao. A URL de webhook precisa ser HTTPS e acessivel publicamente para o Mercado Pago enviar as notificacoes.

5. Inicie:

```bash
npm run dev
```

## Modulos

- Autenticacao JWT com access token, refresh token e roles: `aluno`, `instrutor`, `admin`.
- Pedidos de curso com status, urgencia, fila normal e fila fast-track.
- Prioridade isolada em `src/services/priorityService.js`, com testes unitarios.
- Alteracoes de banco documentadas como scripts SQL em `database/migrations`.
- Pagamentos Mercado Pago Checkout Pro: PIX, cartao e boleto pelo checkout do provedor, webhook validado no backend e liberacao automatica do curso somente apos status `approved`.

Exemplos de payloads em `docs/api-examples.md`.
