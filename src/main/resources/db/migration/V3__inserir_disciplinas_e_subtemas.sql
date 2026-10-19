-- Disciplinas e carga horária do curso teórico-técnico (Res. CONTRAN 789/2020, 45 h no total).

INSERT INTO disciplina (codigo, nome, carga_horaria) VALUES
    ('legislacao',        'Legislação de trânsito',                                   18),
    ('direcao-defensiva', 'Direção defensiva',                                        16),
    ('primeiros-socorros','Noções de primeiros socorros',                              4),
    ('meio-ambiente',     'Meio ambiente e convívio social no trânsito',               4),
    ('mecanica',          'Noções sobre funcionamento do veículo de 2 e 4 rodas',      3);

INSERT INTO subtema (disciplina_id, codigo, nome)
SELECT d.id, s.codigo, s.nome
FROM (VALUES
    ('legislacao', 'habilitacao',                  'Processo de habilitação e CNH'),
    ('legislacao', 'normas-circulacao',            'Normas gerais de circulação e conduta'),
    ('legislacao', 'sinalizacao-regulamentacao',   'Placas de regulamentação'),
    ('legislacao', 'sinalizacao-advertencia',      'Placas de advertência'),
    ('legislacao', 'sinalizacao-indicacao',        'Placas de indicação'),
    ('legislacao', 'sinalizacao-horizontal',       'Sinalização horizontal'),
    ('legislacao', 'sinalizacao-luminosa-gestos',  'Semáforos, gestos e sinais sonoros'),
    ('legislacao', 'infracoes-penalidades',        'Infrações, penalidades e medidas administrativas'),
    ('legislacao', 'crimes-transito',              'Crimes de trânsito'),
    ('legislacao', 'veiculos',                     'Registro, licenciamento e equipamentos obrigatórios'),

    ('direcao-defensiva', 'conceitos-defensiva',   'Conceitos e elementos da direção defensiva'),
    ('direcao-defensiva', 'condicoes-adversas',    'Condições adversas'),
    ('direcao-defensiva', 'evitar-colisoes',       'Como evitar colisões'),
    ('direcao-defensiva', 'estado-condutor',       'Estado físico e mental do condutor'),
    ('direcao-defensiva', 'usuarios-vulneraveis',  'Pedestres, ciclistas e motociclistas'),

    ('primeiros-socorros', 'sinalizacao-acidente', 'Sinalização do local e acionamento do socorro'),
    ('primeiros-socorros', 'avaliacao-vitima',     'Verificação das condições da vítima'),
    ('primeiros-socorros', 'cuidados-vitima',      'Cuidados com a vítima e condutas a evitar'),

    ('meio-ambiente', 'poluicao',                  'Poluição e manutenção do veículo'),
    ('meio-ambiente', 'cidadania',                 'Cidadania e convívio social no trânsito'),

    ('mecanica', 'sistemas-veiculo',               'Sistemas do veículo'),
    ('mecanica', 'manutencao-painel',              'Manutenção preventiva e luzes do painel')
) AS s (disciplina_codigo, codigo, nome)
JOIN disciplina d ON d.codigo = s.disciplina_codigo;
