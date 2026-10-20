export interface TestGeneration {
  id: number;
  sourceFileId: number | null;
  sourceFileName: string | null;
  language: string;
  testFramework: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  result: TestGenerationResult | null;
  errorMessage: string | null;
  modelUsed: string;
  createdAt: string;
}

export interface TestGenerationResult {
  language: string;
  testFramework: string;
  testFiles: TestFile[];
  summary: string;
  assumptions?: string[];
}

export interface TestFile {
  filename: string;
  content: string;
  tests: TestCase[];
}

export interface TestCase {
  name: string;
  description: string;
  category: 'happy_path' | 'edge_case' | 'error_handling' | 'null_safety' | 'integration';
}

export interface GenerateTestsRequest {
  fileId?: number;
  sourceCode?: string;
  language?: string;
  testFramework?: string;
}
