import type { PageResponse } from './github.types'
export type CodingSort = 'startedAt' | 'projectName' | 'language'
export interface CodingSessionResponse { id:number; projectName:string; language:string; startedAt:string; endedAt:string; source:string; notes:string|null; durationSeconds:number; createdAt:string; updatedAt:string }
export interface CodingHistoryParameters { startDate:string; endDate:string; page:number; size:number; sortBy:CodingSort }
export type CodingPage = PageResponse<CodingSessionResponse>
