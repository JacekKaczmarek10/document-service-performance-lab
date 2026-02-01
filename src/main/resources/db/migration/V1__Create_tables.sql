-- Create authors table
CREATE TABLE authors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Create tags table
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Create documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id BIGINT NOT NULL,
    CONSTRAINT fk_document_author FOREIGN KEY (author_id) REFERENCES authors(id)
);

-- Create permissions table
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    document_id BIGINT NOT NULL,
    CONSTRAINT fk_permission_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create junction table for document-tag many-to-many relationship
CREATE TABLE document_tags (
    document_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (document_id, tag_id),
    CONSTRAINT fk_document_tags_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Create indexes for performance optimization
CREATE INDEX idx_document_created_at ON documents(created_at DESC);
CREATE INDEX idx_document_author ON documents(author_id);
CREATE INDEX idx_permission_document ON permissions(document_id);
CREATE INDEX idx_document_tags_document ON document_tags(document_id);
CREATE INDEX idx_document_tags_tag ON document_tags(tag_id);

