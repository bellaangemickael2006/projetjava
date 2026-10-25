-- ============================================================
-- VitaTrack — Schéma Supabase (PostgreSQL)
-- À coller dans : Supabase Dashboard > SQL Editor > New query
-- ============================================================

-- Extension UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── UTILISATEURS ─────────────────────────────────────────────────────
-- Note : auth.users est géré automatiquement par Supabase Auth.
-- Cette table stocke les données métier additionnelles.
CREATE TABLE IF NOT EXISTS utilisateurs (
  id            UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email         TEXT UNIQUE NOT NULL,
  nom           TEXT NOT NULL,
  role          TEXT NOT NULL CHECK (role IN ('admin','medecin','patient')),
  statut        TEXT NOT NULL DEFAULT 'actif' CHECK (statut IN ('actif','inactif')),
  medecin_id    UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  specialite    TEXT,
  date_naissance DATE,
  telephone     TEXT,
  cree_par      UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── HOSPITALISATIONS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hospitalisations (
  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id       UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  medecin_id       UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  diagnostic       TEXT,
  date_entree      DATE,
  date_sortie      DATE,
  medecin_referent TEXT,
  notes            TEXT,
  statut           TEXT DEFAULT 'en_cours' CHECK (statut IN ('en_cours','termine')),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── TRAITEMENTS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS traitements (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id  UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  medecin_id  UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  medicament  TEXT NOT NULL,
  dosage      TEXT,
  frequence   TEXT,
  duree       TEXT,
  instructions TEXT,
  actif       BOOLEAN NOT NULL DEFAULT TRUE,
  version     INT NOT NULL DEFAULT 1,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── RENDEZ-VOUS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rendezvous (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id  UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  medecin_id  UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  date_rdv    DATE NOT NULL,
  heure       TIME,
  motif       TEXT,
  statut      TEXT NOT NULL DEFAULT 'planifie' CHECK (statut IN ('planifie','confirme','annule','effectue')),
  notes       TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── SIGNALEMENTS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS signalements (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id  UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  medecin_id  UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  type        TEXT NOT NULL,
  description TEXT NOT NULL,
  intensite   SMALLINT CHECK (intensite BETWEEN 1 AND 10),
  urgent      BOOLEAN NOT NULL DEFAULT FALSE,
  traite      BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── QUESTIONNAIRES ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS questionnaires (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id  UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  medecin_id  UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  titre       TEXT NOT NULL,
  questions   TEXT,
  reponses    TEXT,
  statut      TEXT NOT NULL DEFAULT 'en_attente' CHECK (statut IN ('en_attente','complete')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── JOURNAL DE SUIVI ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS journal_suivi (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id  UUID NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  medecin_id  UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  date_entree DATE NOT NULL,
  contenu     TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── PASSEPORT SANTÉ ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS passeport_sante (
  id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_id           UUID UNIQUE NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
  groupe_sanguin       TEXT,
  allergies            TEXT,
  maladies_chroniques  TEXT,
  antecedents          TEXT,
  medecin_traitant     TEXT,
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── JOURNAL D'ACTIVITÉ ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS journal_activite (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id     UUID REFERENCES utilisateurs(id) ON DELETE SET NULL,
  action      TEXT NOT NULL,
  details     TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── ROW LEVEL SECURITY ────────────────────────────────────────────────
ALTER TABLE utilisateurs       ENABLE ROW LEVEL SECURITY;
ALTER TABLE hospitalisations   ENABLE ROW LEVEL SECURITY;
ALTER TABLE traitements        ENABLE ROW LEVEL SECURITY;
ALTER TABLE rendezvous         ENABLE ROW LEVEL SECURITY;
ALTER TABLE signalements       ENABLE ROW LEVEL SECURITY;
ALTER TABLE questionnaires     ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_suivi      ENABLE ROW LEVEL SECURITY;
ALTER TABLE passeport_sante    ENABLE ROW LEVEL SECURITY;

-- Politique : service_role peut tout faire (notre backend)
CREATE POLICY "service_role_all" ON utilisateurs     FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON hospitalisations FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON traitements      FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON rendezvous        FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON signalements      FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON questionnaires    FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON journal_suivi     FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "service_role_all" ON passeport_sante   FOR ALL TO service_role USING (TRUE) WITH CHECK (TRUE);

-- ─── COMPTES DE DÉMONSTRATION ──────────────────────────────────────────
-- IMPORTANT : exécuter ces inserts APRÈS avoir créé les utilisateurs
-- dans Supabase Auth (Dashboard > Authentication > Users > Add user)
-- puis noter leurs UUIDs et remplacer les placeholders ci-dessous.
--
-- Ou utiliser la fonction admin ci-dessous :

-- Exemple (remplacer les UUIDs par ceux que Supabase génère) :
/*
INSERT INTO utilisateurs (id, email, nom, role) VALUES
  ('UUID_ADMIN',   'admin@vitatrack.demo',   'Administrateur', 'admin'),
  ('UUID_MEDECIN', 'medecin@vitatrack.demo', 'Dr. Koffi',       'medecin'),
  ('UUID_PATIENT', 'patient@vitatrack.demo', 'Jean Kouassi',    'patient');

UPDATE utilisateurs SET medecin_id = 'UUID_MEDECIN' WHERE id = 'UUID_PATIENT';
*/
