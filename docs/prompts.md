# Prompts Catalog

This document lists the prompts used by CodeDoc AI to interact with LLMs. All prompts are located in the `backend/src/main/resources/prompts/` directory.

## 1. Detect Complexity
- **Name:** `detect_complexity`
- **Version:** `v1`
- **File:** `detect_complexity_v1.txt`
- **Purpose:** Analyzes a given source code file and evaluates its complexity, maintainability, and potential risk areas. It assigns a complexity score from 1-10 and provides actionable recommendations for improvement based on cyclomatic complexity, cognitive complexity, code size, readability, and error handling.

## 2. Generate Documentation
- **Name:** `generate_doc`
- **Version:** `v1`
- **File:** `generate_doc_v1.txt`
- **Purpose:** Reads a source code file and generates a comprehensive, well-structured technical documentation document divided into multiple sections (e.g. summaries, classes, functions, notes). Outputs JSON based on a strictly defined JSON schema.

## 3. Generate Examples
- **Name:** `generate_examples`
- **Version:** `v1`
- **File:** `generate_examples_v1.txt`
- **Purpose:** Analyzes code segments to extract or fabricate practical, functional code usage examples. Helps in building the "EXAMPLE" section types in documentation.

## 4. Summarize Project
- **Name:** `summarize_project`
- **Version:** `v1`
- **File:** `summarize_project_v1.txt`
- **Purpose:** Looks across multiple source code files within a given project to construct a high-level summary. Identifies main architectures, key modules, and the overall objective of the codebase.
