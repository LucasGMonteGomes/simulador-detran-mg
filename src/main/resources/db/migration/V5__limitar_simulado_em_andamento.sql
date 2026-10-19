-- No máximo um simulado em andamento por modo: sempre dá para retomar a prova aberta, e nunca ficam duas
-- disputando o mesmo cronômetro (por exemplo, com um clique duplo em "Iniciar").
CREATE UNIQUE INDEX uq_simulado_em_andamento ON simulado (modo) WHERE finalizado_em IS NULL;
