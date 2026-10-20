package com.codedoc.chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class SourceCitationDto {
    private String filename;
    private Integer startLine;
    private Integer endLine;
    private String snippet;
}
