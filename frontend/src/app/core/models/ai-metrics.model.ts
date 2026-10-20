export interface EndpointMetric {
  endpoint: string;
  totalCalls: number;
  successCount: number;
  errorCount: number;
  successRatePercent: number;
  avgLatencyMs: number;
  totalTokens: number;
}

export interface AiLog {
  id: number;
  endpoint: string;
  status: string;
  promptTokens: number | null;
  completionTokens: number | null;
  totalTokens: number;
  latencyMs: number;
  createdAt: string;
  username: string;
}

export interface AiMetrics {
  totalCalls: number;
  successfulCalls: number;
  failedCalls: number;
  successRatePercent: number;
  avgLatencyMs: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  totalTokens: number;
  estimatedCostUsd: number;
  piiRedactionsCount: number;
  activeModel: string;
  endpointMetrics: EndpointMetric[];
  recentLogs: AiLog[];
  statusDistribution: { [key: string]: number };
}
