-- A V10 criou a coluna user_id como obrigatória, mas a entidade OutboxEvent não tem esse campo (o dono já vai
-- dentro do payload). Com ela, toda linha do Outbox falharia ao ser gravada. A V10 não pode ser alterada.
ALTER TABLE outbox_event DROP COLUMN IF EXISTS user_id;
