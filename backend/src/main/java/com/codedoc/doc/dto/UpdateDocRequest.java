package com.codedoc.doc.dto;

import com.codedoc.doc.DocStatus;

public record UpdateDocRequest(
    DocStatus status,
    String errorMessage
) {}
