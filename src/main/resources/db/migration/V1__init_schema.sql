CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS doc;
CREATE SCHEMA IF NOT EXISTS ai;
CREATE SCHEMA IF NOT EXISTS pub;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS job;
CREATE SCHEMA IF NOT EXISTS cache;

CREATE TABLE app.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(50) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  avatar_url VARCHAR(512),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_users_username UNIQUE (username),
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE app.workspaces (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  owner_id UUID NOT NULL,
  description VARCHAR(500),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES app.users (id)
);

CREATE TABLE app.workspace_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL,
  user_id UUID NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES app.workspaces (id),
  CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES app.users (id),
  CONSTRAINT uk_workspace_members_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE TABLE app.knowledge_bases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  visibility VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_by UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_knowledge_bases_workspace FOREIGN KEY (workspace_id) REFERENCES app.workspaces (id),
  CONSTRAINT fk_knowledge_bases_created_by FOREIGN KEY (created_by) REFERENCES app.users (id)
);

CREATE TABLE doc.documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  kb_id UUID NOT NULL,
  parent_id UUID,
  title VARCHAR(255) NOT NULL,
  slug VARCHAR(255),
  content_format VARCHAR(30),
  current_snapshot_id UUID,
  status VARCHAR(20) NOT NULL,
  created_by UUID NOT NULL,
  updated_by UUID,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_documents_kb FOREIGN KEY (kb_id) REFERENCES app.knowledge_bases (id),
  CONSTRAINT fk_documents_parent FOREIGN KEY (parent_id) REFERENCES doc.documents (id),
  CONSTRAINT fk_documents_created_by FOREIGN KEY (created_by) REFERENCES app.users (id),
  CONSTRAINT fk_documents_updated_by FOREIGN KEY (updated_by) REFERENCES app.users (id)
);

CREATE TABLE doc.document_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  version_no INTEGER NOT NULL,
  content_json JSONB,
  content_markdown TEXT,
  content_html TEXT,
  created_by UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_document_snapshots_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT fk_document_snapshots_created_by FOREIGN KEY (created_by) REFERENCES app.users (id),
  CONSTRAINT uk_document_snapshots_document_version UNIQUE (document_id, version_no)
);

ALTER TABLE doc.documents
  ADD CONSTRAINT fk_documents_current_snapshot
  FOREIGN KEY (current_snapshot_id) REFERENCES doc.document_snapshots (id);

CREATE TABLE doc.document_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  snapshot_id UUID NOT NULL,
  version_no INTEGER NOT NULL,
  change_summary VARCHAR(500),
  created_by UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_document_versions_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT fk_document_versions_snapshot FOREIGN KEY (snapshot_id) REFERENCES doc.document_snapshots (id),
  CONSTRAINT fk_document_versions_created_by FOREIGN KEY (created_by) REFERENCES app.users (id),
  CONSTRAINT uk_document_versions_document_version UNIQUE (document_id, version_no)
);

CREATE TABLE doc.document_collab_updates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  client_id VARCHAR(100) NOT NULL,
  update_data BYTEA NOT NULL,
  seq BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_document_collab_updates_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT uk_document_collab_updates_document_seq UNIQUE (document_id, seq)
);

CREATE TABLE doc.document_collab_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  state_binary BYTEA NOT NULL,
  snapshot_version BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_document_collab_snapshots_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT uk_document_collab_snapshots_document_version UNIQUE (document_id, snapshot_version)
);

CREATE TABLE app.assets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL,
  uploader_id UUID NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_size BIGINT NOT NULL,
  mime_type VARCHAR(100),
  storage_type VARCHAR(20) NOT NULL,
  storage_path VARCHAR(500),
  checksum VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_assets_workspace FOREIGN KEY (workspace_id) REFERENCES app.workspaces (id),
  CONSTRAINT fk_assets_uploader FOREIGN KEY (uploader_id) REFERENCES app.users (id)
);

CREATE TABLE ai.document_chunks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  snapshot_id UUID NOT NULL,
  chunk_index INTEGER NOT NULL,
  heading_path VARCHAR(500),
  content TEXT NOT NULL,
  token_count INTEGER,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT fk_document_chunks_snapshot FOREIGN KEY (snapshot_id) REFERENCES doc.document_snapshots (id),
  CONSTRAINT uk_document_chunks_snapshot_index UNIQUE (snapshot_id, chunk_index)
);

CREATE TABLE ai.document_chunk_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  chunk_id UUID NOT NULL,
  document_id UUID NOT NULL,
  kb_id UUID NOT NULL,
  embedding VECTOR(1536),
  model VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_document_chunk_embeddings_chunk FOREIGN KEY (chunk_id) REFERENCES ai.document_chunks (id),
  CONSTRAINT fk_document_chunk_embeddings_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT fk_document_chunk_embeddings_kb FOREIGN KEY (kb_id) REFERENCES app.knowledge_bases (id),
  CONSTRAINT uk_document_chunk_embeddings_chunk_model UNIQUE (chunk_id, model)
);

CREATE TABLE ai.ai_conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL,
  user_id UUID NOT NULL,
  title VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_ai_conversations_workspace FOREIGN KEY (workspace_id) REFERENCES app.workspaces (id),
  CONSTRAINT fk_ai_conversations_user FOREIGN KEY (user_id) REFERENCES app.users (id)
);

CREATE TABLE ai.ai_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  citations JSONB,
  usage JSONB,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai.ai_conversations (id)
);

CREATE TABLE pub.publication_channels (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL,
  type VARCHAR(30) NOT NULL,
  name VARCHAR(100) NOT NULL,
  app_id VARCHAR(100) NOT NULL,
  app_secret VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_publication_channels_workspace FOREIGN KEY (workspace_id) REFERENCES app.workspaces (id)
);

CREATE TABLE pub.publication_tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  snapshot_id UUID,
  channel_id UUID NOT NULL,
  title VARCHAR(255) NOT NULL,
  digest VARCHAR(500),
  author VARCHAR(100),
  cover_asset_id UUID,
  status VARCHAR(30) NOT NULL,
  external_draft_id VARCHAR(100),
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_publication_tasks_document FOREIGN KEY (document_id) REFERENCES doc.documents (id),
  CONSTRAINT fk_publication_tasks_snapshot FOREIGN KEY (snapshot_id) REFERENCES doc.document_snapshots (id),
  CONSTRAINT fk_publication_tasks_channel FOREIGN KEY (channel_id) REFERENCES pub.publication_channels (id),
  CONSTRAINT fk_publication_tasks_cover_asset FOREIGN KEY (cover_asset_id) REFERENCES app.assets (id)
);

CREATE TABLE audit.audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID,
  actor_id UUID NOT NULL,
  action VARCHAR(50) NOT NULL,
  resource_type VARCHAR(50),
  resource_id UUID,
  before_data JSONB,
  after_data JSONB,
  ip VARCHAR(45),
  user_agent VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_audit_logs_workspace FOREIGN KEY (workspace_id) REFERENCES app.workspaces (id),
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES app.users (id)
);

CREATE TABLE job.jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  payload JSONB,
  error_message TEXT,
  retry_count INTEGER NOT NULL,
  max_retries INTEGER NOT NULL,
  scheduled_at TIMESTAMPTZ,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cache.cache_entries (
  cache_key VARCHAR(255) PRIMARY KEY,
  cache_value JSONB NOT NULL,
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_workspace_members_user_id ON app.workspace_members (user_id);
CREATE INDEX idx_knowledge_bases_workspace_status ON app.knowledge_bases (workspace_id, status);
CREATE INDEX idx_assets_workspace_id ON app.assets (workspace_id);
CREATE INDEX idx_assets_uploader_id ON app.assets (uploader_id);

CREATE INDEX idx_documents_kb_status ON doc.documents (kb_id, status);
CREATE INDEX idx_documents_parent_status ON doc.documents (parent_id, status);
CREATE INDEX idx_documents_created_by ON doc.documents (created_by);
CREATE INDEX idx_document_snapshots_document_version ON doc.document_snapshots (document_id, version_no DESC);
CREATE INDEX idx_document_versions_document_version ON doc.document_versions (document_id, version_no DESC);
CREATE INDEX idx_document_collab_updates_document_seq ON doc.document_collab_updates (document_id, seq);
CREATE INDEX idx_document_collab_snapshots_document_version ON doc.document_collab_snapshots (document_id, snapshot_version DESC);

CREATE INDEX idx_document_chunks_document_id ON ai.document_chunks (document_id);
CREATE INDEX idx_document_chunks_snapshot_id ON ai.document_chunks (snapshot_id);
CREATE INDEX idx_document_chunk_embeddings_document_id ON ai.document_chunk_embeddings (document_id);
CREATE INDEX idx_document_chunk_embeddings_kb_id ON ai.document_chunk_embeddings (kb_id);
CREATE INDEX idx_ai_conversations_workspace_user ON ai.ai_conversations (workspace_id, user_id);
CREATE INDEX idx_ai_messages_conversation_created_at ON ai.ai_messages (conversation_id, created_at);

CREATE INDEX idx_publication_channels_workspace_status ON pub.publication_channels (workspace_id, status);
CREATE INDEX idx_publication_tasks_channel_status ON pub.publication_tasks (channel_id, status);
CREATE INDEX idx_publication_tasks_document_id ON pub.publication_tasks (document_id);

CREATE INDEX idx_audit_logs_workspace_created_at ON audit.audit_logs (workspace_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor_created_at ON audit.audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit.audit_logs (resource_type, resource_id);

CREATE INDEX idx_jobs_status_scheduled_at ON job.jobs (status, scheduled_at);
CREATE INDEX idx_jobs_type_status ON job.jobs (type, status);

CREATE INDEX idx_cache_entries_expires_at ON cache.cache_entries (expires_at);
