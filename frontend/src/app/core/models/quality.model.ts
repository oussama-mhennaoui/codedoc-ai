export interface CodeSmell {
  type: string;
  fichier: string;
  ligne: number;
  gravite: 'critique' | 'moyen' | 'faible';
  description: string;
  suggestionRefactoring: string;
  codeOriginal?: string;
  codeRefactorise?: string;
}

export interface RespectSolid {
  srp: string;
  ocp: string;
  lsp: string;
  isp: string;
  dip: string;
  resume: string;
}

export interface DuplicationMetric {
  pourcentage_estime: number;
  details: string;
}

export interface ComplexityMetric {
  score_cyclomatique: number;
  score_cognitif: number;
  details: string;
}

export interface TechnicalDebtMetric {
  estimation_minutes: number;
  niveau: 'faible' | 'moyen' | 'eleve' | 'critique';
  resume: string;
}

export interface QualityReport {
  id: number;
  sourceFileId: number;
  filename: string;
  scoreGlobal: number;
  technicalDebtMinutes: number;
  technicalDebtRating: string;
  codeSmells: CodeSmell[];
  respectSolid: RespectSolid;
  duplication: DuplicationMetric;
  complexite: ComplexityMetric;
  detteTechnique: TechnicalDebtMetric;
  createdAt: string;
  updatedAt: string;
}

export interface FileQualitySummary {
  fileId: number;
  filename: string;
  scoreGlobal: number | null;
  technicalDebtMinutes: number;
  technicalDebtRating: string;
  codeSmellsCount: number;
  criticalSmellsCount: number;
  analyzed: boolean;
}

export interface ProjectQualitySummary {
  projectId: number;
  projectName: string;
  averageScore: number;
  totalTechnicalDebtMinutes: number;
  totalCodeSmells: number;
  totalCriticalSmells: number;
  fileSummaries: FileQualitySummary[];
}
