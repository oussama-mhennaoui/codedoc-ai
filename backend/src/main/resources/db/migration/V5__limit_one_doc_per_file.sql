-- V5__limit_one_doc_per_file.sql
ALTER TABLE generated_docs ADD CONSTRAINT uq_generated_docs_source_file_id UNIQUE (source_file_id);
