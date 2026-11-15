BEGIN;

CREATE TEMP TABLE demo_users (slug TEXT, display_name TEXT, email TEXT, role TEXT) ON COMMIT DROP;
INSERT INTO demo_users VALUES
    ('estudio-andino', 'Estudio Andino', 'estudio.andino@demo.kino.app', 'CREATOR'),
    ('laura-mendez', 'Laura Méndez Films', 'laura.mendez@demo.kino.app', 'CREATOR'),
    ('colectivo-sonoro', 'Colectivo Sonoro', 'colectivo.sonoro@demo.kino.app', 'CREATOR'),
    ('animarte-lab', 'Animarte Lab', 'animarte.lab@demo.kino.app', 'CREATOR'),
    ('ruta-documental', 'Ruta Documental', 'ruta.documental@demo.kino.app', 'CREATOR'),
    ('viewer-1', 'Camila Torres', 'camila.torres@demo.kino.app', 'CREATOR'),
    ('viewer-2', 'Mateo Ríos', 'mateo.rios@demo.kino.app', 'CREATOR'),
    ('viewer-3', 'Valentina Paz', 'valentina.paz@demo.kino.app', 'CREATOR'),
    ('viewer-4', 'Sebastián Lara', 'sebastian.lara@demo.kino.app', 'CREATOR'),
    ('viewer-5', 'Isabella Cruz', 'isabella.cruz@demo.kino.app', 'CREATOR'),
    ('viewer-6', 'Daniel Vega', 'daniel.vega@demo.kino.app', 'CREATOR');

INSERT INTO public.users (id, display_name, email, password_hash, role, created_at)
SELECT md5('kino-demo-user-' || slug)::uuid, display_name, email,
       '$2a$10$8048g5rLzXuS6MVMhvn29Dq02TRVyLJ48GX76GOTqkwZqRGACeeUQ', role, now() - interval '90 days'
FROM demo_users
ON CONFLICT (email) DO NOTHING;

CREATE TEMP TABLE demo_videos (
    slug TEXT, creator TEXT, title TEXT, description TEXT, video_url TEXT, tags TEXT[],
    views BIGINT, days_ago INTEGER, price NUMERIC(10, 2), donation NUMERIC(10, 2)
) ON COMMIT DROP;
INSERT INTO demo_videos VALUES
    ('sintel', 'animarte-lab', 'Sintel: la búsqueda',
     'Una joven recorre montañas heladas para encontrar al dragón que crió. Animación 3D de cine abierto con una fotografía épica.',
     'https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4', ARRAY['Animación', 'Fantasía', 'Aventura'], 7350, 25, 0, 3.00),
    ('sesiones-azotea', 'colectivo-sonoro', 'Sesiones en la azotea',
     'Tres bandas emergentes tocan en vivo sobre los techos de la ciudad al atardecer. Sonido directo, sin cortes.',
     'https://samplelib.com/lib/preview/mp4/sample-30s.mp4', ARRAY['Música', 'Acústico', 'En vivo'], 6210, 12, 0, 2.00),
    ('conejo-gigante', 'animarte-lab', 'Conejo gigante',
     'Un conejo bonachón decide darle una lección a tres roedores abusivos. Comedia animada para toda la familia.',
     'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', ARRAY['Animación', 'Comedia', 'Familia'], 5630, 33, 0, NULL),
    ('raices-de-agua', 'ruta-documental', 'Raíces de agua',
     'Un viaje por los ríos andinos y las comunidades que los protegen. Documental de naturaleza filmado durante dos años.',
     'https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4', ARRAY['Documental', 'Naturaleza'], 4820, 40, 0, 2.50),
    ('flor-de-medianoche', 'laura-mendez', 'Flor de medianoche',
     'Una flor que solo abre de noche guarda el secreto de una familia. Cortometraje poético rodado en un solo plano.',
     'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', ARRAY['Cortometraje', 'Poesía visual'], 3890, 18, 0, 1.50),
    ('noches-de-bolero', 'colectivo-sonoro', 'Noches de bolero',
     'Un trío de veteranos revive los boleros clásicos en un café del centro histórico.',
     'https://samplelib.com/lib/preview/mp4/sample-20s.mp4', ARRAY['Música', 'Bolero', 'En vivo'], 3420, 45, 0, NULL),
    ('mujeres-del-cafe', 'ruta-documental', 'Mujeres del café',
     'Productoras de café de montaña cuentan cómo transformaron su cooperativa en un referente de comercio justo.',
     'https://samplelib.com/lib/preview/mp4/sample-15s.mp4', ARRAY['Documental', 'Comunidad'], 3105, 15, 2.99, NULL),
    ('ritmo-de-ciudad', 'colectivo-sonoro', 'Ritmo de ciudad',
     'Videoclip urbano grabado en un día de lluvia entre buses, mercados y grafitis.',
     'https://samplelib.com/lib/preview/mp4/sample-20s.mp4', ARRAY['Videoclip', 'Urbano'], 2980, 9, 0, 1.00),
    ('cumbia-periferia', 'colectivo-sonoro', 'Cumbia en la periferia',
     'La nueva cumbia de los barrios: sintetizadores, acordeones y mucho baile.',
     'https://samplelib.com/lib/preview/mp4/sample-10s.mp4', ARRAY['Videoclip', 'Cumbia'], 2210, 30, 0, NULL),
    ('sueno-elefantes', 'laura-mendez', 'El sueño de los elefantes',
     'Dos personajes atrapados en una máquina infinita discuten sobre la realidad. Experimental y surrealista.',
     'https://archive.org/download/ElephantsDream/ed_1024_512kb.mp4', ARRAY['Experimental', 'Ciencia ficción'], 2140, 60, 0, NULL),
    ('iluminar-una-lampara', 'estudio-andino', 'Cómo iluminar con una sola lámpara',
     'Aprende a crear tres atmósferas distintas con una sola fuente de luz y materiales caseros.',
     'https://samplelib.com/lib/preview/mp4/sample-15s.mp4', ARRAY['Tutorial', 'Fotografía'], 1750, 20, 0, 1.00),
    ('ultimos-tranvias', 'laura-mendez', 'Últimos tranvías',
     'El último conductor de tranvía de la ciudad se despide de su ruta. Drama breve y nostálgico.',
     'https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4', ARRAY['Cortometraje', 'Drama'], 1640, 2, 1.99, NULL),
    ('montaje-10-minutos', 'estudio-andino', 'Montaje en 10 minutos',
     'Flujo de edición rápido para redes y festivales: cortes, ritmo y color sin complicaciones.',
     'https://samplelib.com/lib/preview/mp4/sample-10s.mp4', ARRAY['Tutorial', 'Edición'], 1320, 4, 0, NULL),
    ('viaje-de-un-pixel', 'animarte-lab', 'Viaje de un píxel',
     'Un píxel escapa de una pantalla rota y descubre el mundo real. Animación familiar en 2D.',
     'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', ARRAY['Animación', 'Familia'], 1180, 1, 0, NULL),
    ('guardianes-paramo', 'ruta-documental', 'Guardianes del páramo',
     'Guardabosques voluntarios defienden los frailejones del páramo frente a los incendios.',
     'https://samplelib.com/lib/preview/mp4/sample-5s.mp4', ARRAY['Documental', 'Medio ambiente'], 980, 3, 0, 2.00),
    ('luz-liquida', 'animarte-lab', 'Luz líquida',
     'Videoarte hipnótico de medusas y luz. Ideal para proyectar en instalaciones.',
     'https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4', ARRAY['Experimental', 'Videoarte'], 760, 6, 0, NULL);

INSERT INTO public.videos (id, title, description, video_url, thumbnail_url, view_count, access_price,
                           suggested_donation, creator_id, created_at)
SELECT md5('kino-demo-video-' || v.slug)::uuid, v.title, v.description, v.video_url,
       'https://picsum.photos/seed/kino-' || v.slug || '/640/360', v.views, v.price, v.donation,
       u.id, now() - make_interval(days => v.days_ago)
FROM demo_videos v
JOIN demo_users d ON d.slug = v.creator
JOIN public.users u ON u.email = d.email
ON CONFLICT (id) DO UPDATE SET view_count = EXCLUDED.view_count, thumbnail_url = EXCLUDED.thumbnail_url,
                               video_url = EXCLUDED.video_url, description = EXCLUDED.description;

DELETE FROM public.video_tags WHERE video_id IN (SELECT md5('kino-demo-video-' || slug)::uuid FROM demo_videos);
INSERT INTO public.video_tags (video_id, tag)
SELECT md5('kino-demo-video-' || v.slug)::uuid, t.tag
FROM demo_videos v, unnest(v.tags) WITH ORDINALITY AS t(tag, position)
ORDER BY v.slug, t.position;

CREATE TEMP TABLE demo_interactions (viewer TEXT, video TEXT, rating INTEGER, seconds INTEGER, days_ago INTEGER) ON COMMIT DROP;
INSERT INTO demo_interactions VALUES
    ('viewer-1', 'sintel', 5, 52, 3), ('viewer-1', 'conejo-gigante', 5, 10, 4), ('viewer-1', 'sesiones-azotea', 4, 30, 2),
    ('viewer-1', 'raices-de-agua', 4, 10, 6), ('viewer-2', 'sintel', 5, 52, 1), ('viewer-2', 'conejo-gigante', 4, 10, 2),
    ('viewer-2', 'viaje-de-un-pixel', 5, 10, 1), ('viewer-2', 'flor-de-medianoche', 4, 20, 5),
    ('viewer-3', 'sesiones-azotea', 5, 30, 2), ('viewer-3', 'noches-de-bolero', 5, 20, 7),
    ('viewer-3', 'ritmo-de-ciudad', 4, 20, 3), ('viewer-3', 'cumbia-periferia', 4, 10, 8),
    ('viewer-4', 'raices-de-agua', 5, 10, 9), ('viewer-4', 'guardianes-paramo', 5, 5, 2),
    ('viewer-4', 'mujeres-del-cafe', 4, 15, 4), ('viewer-4', 'sueno-elefantes', 3, 40, 12),
    ('viewer-5', 'flor-de-medianoche', 5, 20, 3), ('viewer-5', 'ultimos-tranvias', 5, 10, 1),
    ('viewer-5', 'sueno-elefantes', 4, 40, 6), ('viewer-5', 'luz-liquida', 4, 10, 2),
    ('viewer-6', 'iluminar-una-lampara', 5, 15, 5), ('viewer-6', 'montaje-10-minutos', 5, 10, 2),
    ('viewer-6', 'sintel', 4, 52, 4), ('viewer-6', 'sesiones-azotea', 4, 30, 3);

INSERT INTO public.interactions (id, user_id, video_id, rating, watched_seconds, watched_at)
SELECT md5('kino-demo-interaction-' || i.viewer || '-' || i.video)::uuid, u.id,
       md5('kino-demo-video-' || i.video)::uuid, i.rating, i.seconds, now() - make_interval(days => i.days_ago)
FROM demo_interactions i
JOIN demo_users d ON d.slug = i.viewer
JOIN public.users u ON u.email = d.email
ON CONFLICT (id) DO NOTHING;

COMMIT;
